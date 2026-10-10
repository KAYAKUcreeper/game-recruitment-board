# 掲示板API契約ガイド

正本: [schema/game-recruitment/openapi.yaml](../../../schema/game-recruitment/openapi.yaml)。業務要件: [spec.md](../spec.md)。保存/状態: [data-model.md](../data-model.md)。設計済み、掲示板APIは未実装。

## API一覧（/api配下）

| メソッド | URL | 認証 | 成功/用途 |
| --- | --- | --- | --- |
| GET | /games | 不要 | 200 固定3ゲーム |
| GET | /recruitments | 不要 | 200 受付のみ、game任意、page=1既定、20件 |
| POST | /recruitments | 必須＋CSRF | 201 作成、Location付き公開詳細 |
| GET | /recruitments/{recruitmentId} | 不要 | 200 公開詳細。合流情報を含めない |
| POST | /recruitments/{recruitmentId}/participations/me | 必須＋CSRF | 201 即時参加、更新後公開詳細 |
| POST | /recruitments/{recruitmentId}/participations/me/cancel | 必須＋CSRF | 200 本人キャンセル、更新後公開詳細 |
| POST | /recruitments/{recruitmentId}/close | 必須＋CSRF | 200 手動締め切り、更新後公開詳細 |
| POST | /recruitments/{recruitmentId}/reopen | 必須＋CSRF | 200 空きのある手動締め切りを再開 |
| POST | /recruitments/{recruitmentId}/cancel | 必須＋CSRF | 200 開催中止、更新後公開詳細 |
| GET | /recruitments/{recruitmentId}/meeting-info | 必須 | 200 許可された人だけ合流情報 |
| POST | /recruitments/{recruitmentId}/meeting-info | 必須＋CSRF | 200 募集者による登録/更新/未登録へ戻す |
| GET | /me/recruitments | 必須 | 200 kind/periodによる本人の各区分、20件 |

成功応答はcommit完了後。公開詳細は合流情報・認証情報を含めない。合流情報は`registered`と`text`を返し、未登録ではfalse/空文字。画面に「合流情報は未登録」と表示する。
参加/キャンセル/管理の成功後は最新公開詳細を表示し、既に開いた別画面は再表示時に更新する。200/201の内容は当該処理の成立時点のsnapshotであり、後続操作による即時変化をpushしない。

## 認証とCSRF

[認証契約](../../../schema/auth/openapi.yaml)のauth_token、XSRF-TOKEN、X-XSRF-TOKENと、期限/失効/Originを再利用。
GET /auth/csrfはauth契約の操作であり、掲示板契約に重複定義しない。
全POSTは同一security requirement内のAuthCookie＋CsrfCookie＋CsrfHeader（AND）。公開GETはsecurity=[]、不正JWTでも公開情報を取得可能。
合流情報と本人一覧はAuthCookieのみ必須。401でログインへ、403で権限またはCSRFの理由を表示。CSRF_INVALIDの場合だけ再取得を行い、業務権限403ではCSRF失敗と混同しない。POSTの自動再送はしない。
全API応答no-store、秘密情報のJSON/ログ出力なし。セッション、refresh token、任意OriginのCORSは追加しない。

## エラー

形はauthと同じ`code/message/fields`。掲示板は独立したcode/項目enumを持つ。fieldsは項目名のみ。

| HTTP | code | 意味 |
| --- | --- | --- |
| 400 | VALIDATION_ERROR | JSON/文字数/ゲーム/整数/日時/UUID/queryが無効 |
| 401 | UNAUTHENTICATED | JWTなし、不正、期限切れ、失効、本人不在 |
| 403 | CSRF_INVALID | Cookie/header/Origin不一致 |
| 403 | NOT_OWNER | 募集者だけの操作を他人が要求 |
| 403 | MEETING_INFO_FORBIDDEN | 合流情報の閲覧権限なし |
| 404 | RECRUITMENT_NOT_FOUND | 有効UUIDだが募集が存在しない |
| 409 | OWN_RECRUITMENT / ALREADY_PARTICIPATING / NOT_PARTICIPATING | 本人関係の制約 |
| 409 | RECRUITMENT_FULL / RECRUITMENT_CLOSED / RECRUITMENT_CANCELLED / RECRUITMENT_STARTED | 状態/期限による不可 |
| 409 | NOT_MANUALLY_CLOSED | 再開対象が手動締め切りでない |
| 405 | METHOD_NOT_ALLOWED | 未対応のメソッド、Allow付き。本文編集/削除APIは設けない |
| 415 | UNSUPPORTED_MEDIA_TYPE | 本文ありの操作のContent-Type不適合 |
| 500 | INTERNAL_ERROR | 内部詳細なし |
| 503 | SERVICE_UNAVAILABLE | DB/transaction障害。処理成功を表示しない |

無効JSONを正常型へ暗黙変換しない。参加等のbody不要POSTはrequestBodyを定義しない。複数条件に違反する要求は、CSRF/Origin→認証→JSON/path/query→存在→本人の権限→現在状態の順で確認してよい。未存在のprivate要求でも認証を省略しない。

## HTTP/画面補完方針

- 重複参加・未参加のキャンセルは409。人数は変えない。開始後のキャンセルも409。
- closeは開始前・中止前なら既に手動締め切りでも200。中止の再要求は409、再開していない募集へのreopenも409。
- participantsはjoinedAt順（同時は本人ID）で表示名の重複を許容。UUIDで本人判定。
- マイページはkind=created/participating、period=upcoming/pastを明示。UIは4区分を表示し、次ページを区分ごとに管理。
- 一覧/詳細のゲーム・タイトル・文章・表示名はプレーンテキストで表示。textareaで改行を保持し、改行が保存される境界/ブラウザテストを実施。
- 存在しない募集は専用表示、空の一覧は空である説明。エラー時の人数や成功状態を推測して書き換えず、再表示する。
- 画面: `/`一覧、`/recruitments/new`作成、`/recruitments/:id`詳細、`/mypage`本人一覧。既存`/register`・`/login`を維持。history/popstateの最小ルーティングを使い、専用Router依存は必須にしない。
- ログアウト/401/期限到達・参加キャンセル時にprivate合流情報の画面stateを消す。公開情報は維持。保存コピーを消す機能はない。

## 生成・認証統合の注意

別Maven executionの出力は`backend/target/generated-sources/game-recruitment/`、packageは`com.gamerecruitment.recruitment.adapter.web.generated.api/model`。既存authの出力は維持。
interfaceOnly/skipDefaultInterface/useSpringBoot4/useJackson3をauthと同じにし、検証をスキップしない。生成DTOはweb adapter専用。
Unicode文字数とconst、未知JSON、整数の型、未来時刻、権限は生成annotationだけに依存せずdomain/HTTPテストで保証。

現在のauth生成Errorのenumへ新しい掲示板code/fieldsを渡さない。共有web envelopeと機能ごとの変換に分け、auth応答の互換性を回帰テストで維持。
CurrentAccountFilter/SecurityConfigurationに保護経路を追加し、公開GETの認証スキップと私有GETの保護を先にテストする。

## 検証状況

設計時の静的検証結果をここへ追記する。Generatorのvalidate/生成成功は業務動作や実APIの合格を意味しない。
実装後は[quickstart](../quickstart.md)に従い、Schemathesisと独立した振る舞いテストの両方を実行する。


### 設計時の静的検証結果（2026-10-10）

- OpenAPI Generator CLI 7.26.0: `validate`成功、`No validation issues detected.`。
- Schemathesis 4.30.1: 文書を読み込み、全12操作を確認。実APIには接続していない。
- JSON Schema Draft 2020-12: 12 component schema、23件のexample、Unicode/文字数/ゲーム/定員/日時の形式境界、securityのANDと公開DTOの分離を確認。
- authと同じ設定でSpring API interface 5個＋model 12個を`/tmp`へ一時生成し成功。
- 生成した17 JavaファイルをJava 25と既存auth jarの依存classpathでコンパイルし成功。deprecated APIの注記のみ。新しいMaven executionへの統合・手書きcontrollerのコンパイルは実装段階で検証する。
- 掲示板APIの実応答・業務ロジック・競合・ブラウザ/手動受け入れは未実施。生成物はリポジトリへ追加しない。
- 検証した契約のSHA-256: `57cab3d3f60fdd0811733af1d53c22d34632803cd9d96b594d6254030af9a7bf`。

再検証（リポジトリルートから）:

```bash
docker run --rm --network none \
  --mount type=bind,src="$PWD/schema/game-recruitment",dst=/schema,readonly \
  openapitools/openapi-generator-cli:v7.26.0 validate -i /schema/openapi.yaml
```

JSONの型は明示的に検証し、生成DTO/Jackson既定の小数→整数や数値→文字列のcoercionへ依存しない。認証の既存応答と入力を保つ設定範囲を実装時に検証する。
private GETの行ロックはreadOnly=falseのtransactionで実行する。
