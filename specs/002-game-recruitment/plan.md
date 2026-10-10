# Implementation Plan: ゲーム募集掲示板 初版

**Branch**: `plan-game-recruitment` | **Date**: 2026-10-10 | **Spec**: [spec.md](spec.md)

**Input**: 掲示板仕様、[共通設計方針](api-design-decision.md)、実装済み[認証計画](../001-auth/plan.md)。Spec Kit対象は`specs/002-game-recruitment`。スクリプトが返す論理名`002-game-recruitment`と実際のGitブランチ名を区別する。

## Summary

3ゲームの募集検索、募集作成、承認なしの参加・キャンセル、募集者の受付管理・中止・合流情報、マイページを既存アプリに追加する。
同じ募集の変更をPostgreSQLの親行ロックで直列化し、参加行数とClockから現在の状態を判断する。
公開情報と合流情報を別エンドポイントにし、JWT/CSRFの既存基盤を拡張する。
本段階は設計と契約のみ。掲示板コード・マイグレーション・tasks.mdはまだ作成しない。

## Technical Context

**Language/Version**: 既存Java 25（Temurin 25.0.4.1+1）、TypeScript 5.9.3 strict。

**Primary Dependencies**: 既存Spring Boot 4.1.1、MVC/Security/JDBC、Flyway、JOSE、React/React DOM 19.3.0、Vite 8.3.4、Maven Wrapper 3.10.0。新しいフレームワークは追加しない。

**Storage**: PostgreSQL 18.4、既存Docker Compose/named volume。`accounts`を参照する`recruitments`と`participations`の2テーブルを追加。ゲーム一覧はJava enumで固定し、ゲーム管理テーブルは作らない。

**Code Generation**: OpenAPI 3.1.1、既存Generator 7.26.0。authと別のMaven executionで`schema/game-recruitment/openapi.yaml`から外側のSpring interface/DTOを生成。別出力ディレクトリとpackageにし、既存auth生成を上書きしない。

**Testing**: JUnit、MockMvc、Testcontainers PostgreSQL、ArchUnit、Vitest/Testing Library、Playwright、Schemathesis＋pytest。既存依存とlockを使う。

**Target Platform**: PC上のJava/ReactとDocker内のDB。通常HTTPS同一オリジン。localのVite :5173→API :8080、contractの:5174→:8081。対象ブラウザは現行Chrome/Firefox。時刻表示・入力は常にAsia/Tokyo。

**Project Type**: 単一バックエンド・単一React SPA。認証と掲示板を機能packageで分離する。

**Performance Goals**: SC-001〜003の利用時間、SC-006の初見評価は手動受け入れ目標。SC-004の残り1枠/10並列参加、SC-005の状態・権限・時刻境界、SC-007の2回初期化を自動検証。独自の応答時間SLAは追加しない。

**Constraints**: 定員は募集者を除く1〜9人。公開後の本文編集・募集削除・通知・承認はなし。認証失効確認、全POSTのCSRF/Origin照合、no-storeを維持。内側へSpring/JDBC/生成DTOを持ち込まない。入力はサーバーでコードポイント数を検証。

**Scale/Scope**: 7ユーザーストーリー、FR-004〜024、12 API操作。一覧20件/ページ、マイページも各区分20件/ページ。募集本体は2テーブル。履歴の自動削除はしない。

## Constitution Check

| 原則 | 調査前のゲート | 設計後の確認 |
| --- | --- | --- |
| I 仕様先行 | PASS: 既存specに機能・境界・権限あり | PASS: 業務要件を維持し設計/契約のみ作成 |
| II 内向き依存 | PASS: 既存の機能packageに追加可能 | PASS: repository/query portとClockを内側に置き、SQL/transaction/generated DTOは外側 |
| III 責務・簡潔さ | PASS: 単一アプリで対応可能 | PASS: 2テーブル、行ロック、導出状態。Redis、イベント基盤、汎用CQRS frameworkなし |
| IV テスト可能性 | PASS: 受け入れ例と時刻境界あり | PASS: pure状態/権限判断＋差し替えClock、競合だけ実DBで検証 |
| V 入力検証 | PASS: 必須/範囲/固定値が定義済み | PASS: JSON境界とdomain双方、Unicode/整数/日時/未知項目のテストを計画 |

未解決の設計判断なし。受け入れ基準の達成は実装後に測定する。

## Phase 0: Research

[research.md](research.md)に、競合、最新読み取り、時刻判定、認証統合、API/エラー、日時入力、固定ゲーム、生成・契約検証の判断を記録。
既存バージョンを維持し、新しい依存を導入する判断はない。

## Phase 1: Design & Contracts

- [data-model.md](data-model.md): 保存形式、関連、入力条件、状態優先順、読み取り/更新の整合性。
- [contracts/README.md](contracts/README.md): 正本への参照、エンドポイント/HTTPコード、エラーの順序、画面・認証統合。
- [OpenAPI正本](../../schema/game-recruitment/openapi.yaml): 12操作の入力・出力・セキュリティ・エラー。
- [quickstart.md](quickstart.md): 実装後に実行する受け入れシナリオと検証手順。設計時点の検証結果を区別して記録。

## Project Structure

### Documentation (this feature)

```text
specs/002-game-recruitment/
├── spec.md
├── api-design-decision.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/README.md
└── checklists/requirements.md
schema/game-recruitment/
├── README.md
└── openapi.yaml
```

`tasks.md`は次の`$speckit-tasks`で作成する。

### Source Code (implementation targets)

```text
backend/pom.xml                         # 第2のOpenAPI生成execution
backend/src/main/java/com/gamerecruitment/
├── recruitment/domain/                # Game, Recruitment, State, Input, Permissions
├── recruitment/application/           # 検索/作成/参加/管理/合流情報のusecase
│   └── port/                          # RecruitmentRepository, RecruitmentQueries
├── recruitment/adapter/persistence/   # JDBC、transaction decorator
├── recruitment/adapter/web/           # 生成interfaceを実装するcontroller、例外変換
├── shared/adapter/web/                 # code/message/fieldsの共通JSON境界
├── account/adapter/security/           # 保護URLを拡張、既存JWT確認を再利用
└── configuration/                     # SecurityConfigurationとRecruitmentConfiguration
backend/src/main/resources/db/migration/
├── V3__create_recruitments.sql
└── V4__create_participations.sql
backend/src/test/java/com/gamerecruitment/recruitment/
├── domain/
├── application/
├── contract/
└── integration/
frontend/src/features/recruitment/
├── api/                               # 契約型、入力と応答の実行時検証
├── pages/                             # 一覧、詳細、作成、マイページ
└── components/                        # 参加/受付操作、合流情報
frontend/src/shared/api/               # エラーcode/fieldsを両契約に対応
frontend/src/App.tsx                   # 既存登録/ログインを保ち募集画面を追加
frontend/tests/recruitment.spec.ts
frontend/tests/auth.spec.ts            # 認証の回帰
tests/contract/test_recruitment_contract.py
backend/src/test/resources/fixtures/reset_contract.sql
backend/src/test/java/com/gamerecruitment/account/ApiTestSupport.java # FK追加後の共通初期化
```

**Structure Decision**: 内側の更新portは募集スナップショットと参加関係を扱う。query portは一覧/公開詳細/本人別一覧を取得する。抽象化はこの2つに限定し、汎用base repositoryや別サービスを作らない。公開表示名はquery adapterがaccountsのid/display_nameだけを取得する。募集の内側はauthのハッシュやJWTに依存しない。

## Implementation Flow

1. **基盤**: マイグレーション、domain入力/状態/権限、2つのport、Clock、transaction wiring、別契約のDTO生成。
2. **公開検索と作成（US1/2）**: 固定ゲーム、一覧/詳細のquery、作成UI、認証保護。
3. **参加と合流情報（US3/4）**: ロック下の即時参加・キャンセル、重複/上限、private閲覧。
4. **管理（US5）**: close/reopen/cancel/meeting更新。同一ロック下で現在の状態を再判定。
5. **マイページ（US6）**: 作成/参加、開始前/過去の4区分。再表示で最新状態を読む。
6. **結合と初期化（US7）**: 認証回帰、契約、ブラウザ、競合、初期化・手動検証。新FKに合わせ、既存ApiTestSupportのTRUNCATEも関連4表を同時に明示する。authだけの初期化でFKエラーを起こさない。

## Transaction and Time Decisions

- 変更は親募集行を`FOR UPDATE`で取得してから、別SQLで参加行数と本人の参加を再読込する。全変更経路が同じ手順を守る。
- Clockはロック待機後に読む。トランザクション終了直前にも開始前の条件を再確認し、期限到達ならrollbackする。成功応答はcommit完了後。
- 「成立」はトランザクション内の最終条件検証を線形化点とする。要求受信時の時刻で決めない。物理commitとClock判定を原子的に同時実行する保証を主張しない。
- FULL/STARTEDは保存せず、cancelled→started→manualClosed→full→openの順に導出する。キャンセルは参加行を削除し、手動フラグは変更しない。
- 公開詳細は単一SQLのsnapshotで人数/参加者を取得し、同じClock値で状態を導出。公開一覧は受付条件の絞り込みと21行取得を同一SQLで行う。
- private合流情報のtransactionはロックを許可するためreadOnly=falseとし、親行を`FOR SHARE`、その後に本人の参加と現在時刻を確認。更新との順序を確定する。既に返したコピーの削除はしない。

## Integration with Authentication

既存CurrentAccountFilterは`/api/auth/me`だけを保護し、SecurityConfigurationはauth以外をdenyAllしているため、掲示板の保護対象を明示的に追加する。
公開GETは認証Cookieが無効でも拒否しない。合流情報のGETとマイページは認証必須、全POSTは認証＋既存CSRF/Origin必須。
認証フィルターは公開GET/認証公開経路をスキップし、保護対象では既存の署名・期限・失効・アカウント存在確認を使う。

既存ApiExceptionHandlerのauth生成Error enumに掲示板のcode/fieldsを渡さない。共通web境界の単純なJSON envelope（String code/message、項目名の配列）と、機能ごとの例外→許可code/HTTPへのマッピングを設計する。authの応答内容は既存契約を維持し、両機能の回帰テストで確認する。共有frontendエラーvalidatorも両契約の和集合を認識する。

## Requirement Coverage

| 要件/検証目標 | 設計先 | 検証 |
| --- | --- | --- |
| FR-004/005/009/010・US1 | games/list/detail、公開query | 未ログイン・不正Cookie、ゲーム、受付のみ、20件、順序、公開情報のみ |
| FR-006/007/008・US2 | CreateRecruitment、入力/保存、作成画面 | 境界、未来、分単位、JST、0人、本文編集/削除なし |
| FR-011/012・US3・SC-004 | JoinRecruitment、親行ロック＋複合PK | 募集者拒否、重複、10並列最後1枠、人数一致 |
| FR-013/014・US4 | CancelParticipation、行DELETE | 開始前、満員の自動受付、manual/cancel維持、再参加、他人は不可 |
| FR-015〜018・US5・SC-005 | 状態判断、Close/Reopen/CancelRecruitment | 状態優先順、開始境界、満員から手動締切、再開空き、中止不可逆 |
| FR-019/020・US3/5・SC-005 | Get/UpdateMeetingInfo、private lock/read | 閲覧表全ケース、空白未登録、1000境界、漏えいなし |
| FR-021/023・US6 | MyRecruitmentsQuery | 4区分・順序、キャンセル除外、中止/過去保持、再ログイン/再接続 |
| FR-022 | JWT filter、domain権限、時刻再確認 | 401/403/404/409、古い画面、失効DB503、状態不変 |
| FR-024・US7・SC-007 | 専用DB初期化SQL | 子→親→accountsの整合、同じIDで2回、通常APIなし、ゲーム固定一覧保持 |
| SC-001/002/003/006 | 手動＋ブラウザシナリオ | 1分/5分/30秒と初見5人。実測前は未達成扱い |

## Complexity Tracking

憲章違反なし。行ロックは同時参加と状態操作の整合に必要。read portは一覧/結合結果の取得責務に必要な範囲で、CQRS製品や別read DBは導入しない。

## Validation Status

設計ゲートは通過。契約の静的確認結果は[契約ガイド](contracts/README.md)へ記録する。
掲示板実装・実API契約・並列参加・ブラウザ検証は未実施。次は`$speckit-tasks`。
