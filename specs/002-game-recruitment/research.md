# 掲示板機能の設計調査

調査日: 2026-10-10。対象: [spec](spec.md)、[plan](plan.md)。既存バージョンを継続し、新しいライブラリは不要。

## 1. 競合と人数

- Decision: READ COMMITTED transactionで同じ募集の親行を最初にSELECT FOR UPDATEし、参加・キャンセル・close/reopen/cancel・合流情報更新すべてで同じ順序を使う。
- Rationale: 最後の1枠、二重キャンセル、手動締切とキャンセルを同じ仕組みで守る。ロック後の別SELECTで人数と本人の参加を読む。ロック取得とCOUNTを混ぜず、待機前statement snapshotによる古い判断を避ける。
- Decision: participation複合PK、人数は参加行COUNT。キャンセルはDELETE、本人はJWTのUUIDから決める。
- Alternatives considered: Java synchronizedだけでは複数プロセスに効かない。保存カウンターは二重管理が増える。SERIALIZABLE/楽観versionはretry設計が増えるため今回は使わない。
- Sources: [PostgreSQL行ロック](https://www.postgresql.org/docs/18/explicit-locking.html)、[分離レベル](https://www.postgresql.org/docs/18/transaction-iso.html)、[複合PK/外部キー/CHECK](https://www.postgresql.org/docs/18/ddl-constraints.html)。

## 2. 時刻と状態

- Decision: 注入Clockをロック待機後と最終条件確認で読む。成功はTransactionTemplateのcommit完了後。domainはInstant/UUID/count/flagsだけで判断する。
- Rationale: DB now()はtransaction開始時刻であり、待機後の時刻境界の判定には使わない。成立の判定点はtransaction内の最終検証時点。受信時に未来だっただけでは作成/操作を許可しない。物理commit完了時刻と条件確認の原子的一致は保証しない。
- Decision: cancelled/manualClosedを保存し、中止→開始済み→manual→full→openを都度導出する。
- Alternatives considered: FULL/STARTEDの保存＋定期jobは状態同期を増やす。単に受信時のClockを使うのは待機後の開始時刻を見逃す。
- Sources: [PostgreSQL時刻関数](https://www.postgresql.org/docs/18/functions-datetime.html)、[Spring programmatic transaction](https://docs.spring.io/spring-framework/reference/data-access/transaction/programmatic.html)。

## 3. 公開/非公開読み取り

- Decision: 公開詳細の人数・参加者・親情報は単一SQL snapshot。合流情報は公開DTOに含めず、認証付きの別GETで親行FOR SHARE後、別SQLの参加資格とClockを検証。
- Rationale: READ COMMITTEDの複数SELECTで人数と配列が食い違うことを防ぐ。private読取と参加取消/中止/合流情報更新の順序を確定する。
- Alternatives considered: 公開JSONに含めて画面だけ隠すのは不可。全公開GETのロックは必要ない。既存コピーの削除は要件外。
- Sources: [READ COMMITTED snapshot](https://www.postgresql.org/docs/18/transaction-iso.html)、[FOR SHAREの競合](https://www.postgresql.org/docs/18/explicit-locking.html)。

## 4. 認証統合とエラー

- Decision: 既存Cookie/JWT/失効照会/CSRFを使う。現在auth/meのみのfilterとdenyAllの設定をHTTPメソッド・URL単位で拡張。公開GETは不正JWTを無視し、private GETと全POSTは本人確認。
- Decision: auth生成Error enumに掲示板code/fieldsを混ぜず、共通web envelopeと機能ごとの例外マッピングに分ける。authのJSON契約と回帰を維持する。共有TS validatorには両契約のcode/fieldsを追加する。
- Rationale: 現行ApiExceptionHandlerはauthの生成enumに依存しているため、そのまま掲示板の項目を渡すとenum変換に失敗する。内側は技術/HTTP型に依存させない。
- Alternatives considered: auth契約を掲示板項目で拡張しない。別認証システムや掲示板用tokenは作らない。
- Sources: 現行SecurityConfiguration/CurrentAccountFilter/ApiExceptionHandler、[Spring Security filter architecture](https://docs.spring.io/spring-security/reference/servlet/architecture.html)、[OpenAPI security requirementのAND/OR](https://spec.openapis.org/oas/v3.1.1.html#security-requirement-object)。

## 5. 日時と文字

- Decision: 入出力はoffset付きRFC3339 date-time、内部Instant/DB timestamptz、応答Z。日時入力は分単位（秒/小数秒0）。ReactはJSTの入力へ+09:00を明示し、表示にAsia/Tokyoを指定する。
- Decision: Unicodeコードポイント、White_Space集合、不正Unicode/型の拒否はauthの方針に合わせる。タイトル/本文/合流情報はtextareaで改行を保持する。合流情報の空白のみは未登録の空文字。
- Rationale: OSのタイムゾーンやUTF-16長で条件が変わらない。date-time形式では未来性・成立時刻・本人権限は表せないため、domain/applicationで確認する。
- Alternatives considered: timezoneなしJSON、String.length/maxlength/@Sizeだけの検証、JSON小数や文字列のcapacityへの暗黙変換は不採用。
- Sources: [JSON Schema date-time](https://json-schema.org/draft/2020-12/json-schema-validation#section-7.3.1)、[RFC3339](https://www.rfc-editor.org/rfc/rfc3339)、[PostgreSQL日時型](https://www.postgresql.org/docs/18/datatype-datetime.html)。

## 6. 一覧と固定ゲーム

- Decision: ゲーム3値はdomain enumとGET /games、保存CHECKで固定。一覧はpage>=1、20件固定、21行目でhasNext。未来はstartsAt/createdAt/id昇順、過去は降順。
- Rationale: 最大9人と学習目的に十分単純。マイページはkind/periodによる4区分で同じpaging形を使う。同一時点の順序を固定し、更新が入った別ページ間のsnapshotは保持しない。
- Alternatives considered: 編集可能ゲーム表、cursor署名、総件数用の別query、read専用DBは不要。
- Sources: [PostgreSQL LIMIT/OFFSETと一意ORDER BY](https://www.postgresql.org/docs/18/queries-limit.html)。

## 7. コード生成と検証

- Decision: 既存OpenAPI 3.1.1/Generator 7.26.0の別execution、別output/package。interfaceOnly/skipDefaultInterface/useSpringBoot4/useJackson3/useBeanValidation=falseと外側DTOを維持する。
- Decision: Schemathesisは既存5 checks（server error、status、content type、headers、schema）を継続。操作ごとの新規募集/本人/CSRFを用意し、成功/権限/時刻/競合は明示シナリオも実行する。
- Rationale: スキーマ上妥当でも過去日時や無資格で拒否されるので、positive_data_acceptanceを無条件に追加しない。生成成功と実API・業務動作は別の検証。
- Alternatives considered: 新Generator/認証方式への移行、生成DTOをdomainへ持ち込む、拒否応答だけで契約検証を終える方式は不採用。
- Sources: [Spring generator](https://openapi-generator.tech/docs/generators/spring/)、[Schemathesis checks](https://schemathesis.readthedocs.io/en/stable/reference/checks/)、[stateful検証](https://schemathesis.readthedocs.io/en/stable/guides/stateful-testing/)。

## 調査完了

未解決のNEEDS CLARIFICATIONなし。時刻の成立判定点はspecのAssumptionsへ明記し、実装に先行して記録する。実装後の検証手順はquickstartに委ねる。
