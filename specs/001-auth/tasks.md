# Tasks: アカウント 初版

**Input**: `specs/001-auth/` の設計資料と `schema/auth/openapi.yaml`

**Prerequisites**: [plan.md](plan.md)、[spec.md](spec.md)、[research.md](research.md)、[data-model.md](data-model.md)、[契約](../../schema/auth/openapi.yaml)、[quickstart.md](quickstart.md)

**Tests**: 仕様のAcceptance Scenarios・Edge Cases・Success Criteriaを検証するタスクを含む。設計の静的検証と実装の動作検証を区別する。

**Organization**: US1はアカウント登録、US2はログイン・ログアウト。掲示板本体は別機能で管理する。

## Format: `[ID] [P?] [Story] Description`

- `[P]`は、記載した先行条件を満たせば異なるファイルで並行できるタスク。
- USラベルはspecのユーザーストーリーに対応する。準備・共通基盤・横断作業には付けない。
- 全チェックボックスは未着手。タスク分解の完了を実装の完了として扱わない。

## Path Conventions

- Java本体: `backend/src/main/java/com/gamerecruitment/`
- Javaテスト: `backend/src/test/java/com/gamerecruitment/`
- React: `frontend/src/`、ブラウザテスト: `frontend/tests/`
- API契約の正本: `schema/auth/openapi.yaml`。`specs/001-auth/contracts/README.md`から参照する。
- 既存ブランチは `plan-account`。ブランチ変更が必要な場合は、ユーザーに確認してから行う。

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: 合意した構成を再現できる最小の開発環境を用意する。

- [ ] T001 `specs/001-auth/quickstart.md`にTemurin Java 25と既存Node/Dockerの確認・導入手順を具体化し、選定系列の修正版と互換性を確認して環境構築結果を記録する（インストールが権限制限に触れる場合は承認を求める）。
- [ ] T002 Java 25、Spring Boot 4.1.1、Maven Wrapper 3.10.0で `backend/pom.xml`、`backend/mvnw`、`backend/.mvn/`、`backend/src/main/java/com/gamerecruitment/Application.java`を作成し、MVC・Security・JDBC・JOSE・Flyway/PostgreSQL・Bouncy Castleと計画のテスト依存を設定する。OpenAPI GeneratorのMaven pluginを対応版に固定し、`schema/auth/openapi.yaml`を正本としてgenerate-sourcesで仕様検証・Spring API interface/HTTP DTO生成を実行する。検証スキップとデフォルト未実装メソッドを無効にし、契約ガイドのBoot 4設定・外側package・target出力を採用する。生成とコンパイルを実行し、3.1.1の制約の扱いを確認して結果をquickstartへ記録する。
- [ ] T003 [P] React 19.3、TypeScript 5.9 strict、Vite 8の `frontend/package.json`、`frontend/package-lock.json`、`frontend/tsconfig.json`、`frontend/vite.config.ts`、`frontend/src/main.tsx`を作成し、/apiのlocalhost:8080へのproxy、loopback起動、typecheck/test/build/test:e2eスクリプトを設定する（T001後、T002と並行可）。
- [ ] T004 [P] `compose.yaml`と`.env.example`を作成し、PostgreSQL 18系の修正版タグ・digest、dbサービス、127.0.0.1:5432、healthcheck、postgres_data volumeと/var/lib/postgresqlへのマウントを設定する（T001後、T002/T003と並行可）。
- [ ] T005 `.gitignore`と `backend/src/main/resources/application.yaml`、`backend/src/main/resources/application-local.yaml`に秘密設定の除外、DB接続、JWT鍵、許可Origin、通常HTTPS/Secureとloopback専用localの切り替えを設定し、既存の除外項目は維持する。

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: 登録・認証の両方で使う依存境界とHTTP基盤を用意する。

**Checkpoint**: Phase 1とこの基盤を完了してからユーザーストーリーへ進む。

- [ ] T006 `backend/src/main/java/com/gamerecruitment/account/application/port/`にAccountRepository、PasswordHasher、TokenIssuer、RevokedTokenStoreの契約を作成し、`backend/src/main/java/com/gamerecruitment/configuration/AccountConfiguration.java`でClockのBeanを定義する。外側の実装との接続は、登録関連はT019、認証関連はT032で各実装の完成後に行う。内側の型にSpring/JDBC/JWTの型を公開しない。
- [ ] T007 [P] `backend/src/main/java/com/gamerecruitment/account/adapter/web/ApiExceptionHandler.java`に400/401/403/409/415/500/503とcode/message/fieldsの共通応答を実装する。fieldsは項目名のみ、秘密値は返さず、全応答no-storeとする（T006後）。
- [ ] T008 [P] `backend/src/main/java/com/gamerecruitment/account/adapter/security/CsrfSupport.java`、`backend/src/main/java/com/gamerecruitment/account/adapter/web/CsrfController.java`にCookieCsrfTokenRepositoryのSPA対応、raw token取得、GET /api/auth/csrf、全POSTのheader照合、Origin照合を実装する（T006後、T007と並行可）。
- [ ] T009 `backend/src/main/java/com/gamerecruitment/configuration/SecurityConfiguration.java`でJWT認証用セッションを作らない設定、csrf/register/login/logoutの公開経路とmeの認証経路を分離する。CSRFを無効化せず、古い不正Cookieで公開経路を遮断しない。auth_token/XSRF-TOKENの属性は契約ガイドどおりにする（T007/T008後）。
- [ ] T010 [P] `frontend/src/shared/api/client.ts`と`frontend/src/shared/api/validation.ts`に相対URL、same-origin Cookie、CSRF取得・header設定、応答の実行時検証、401/403/503の扱いを実装する。POST自動再送とJWTのlocalStorage保存は行わない（T006後、Java基盤と並行可）。
- [ ] T011 [P] `backend/src/test/java/com/gamerecruitment/architecture/DependencyTest.java`にdomain/applicationからSpring・JDBC・adapterへの依存禁止を検証するArchUnitテストを作成する（T006後）。
- [ ] T012 `backend/src/test/java/com/gamerecruitment/contract/CsrfContractTest.java`と`frontend/src/shared/api/client.test.ts`でcsrf取得、Cookie/raw header一致、欠落・不一致・別Originの403、エラー非漏えいを検証する（T007〜T010後）。

## Phase 3: User Story 1 - アカウントを登録する (Priority: P1)

**Goal**: 公開プロフィールを持つアカウントを登録できる。自動ログインはしない。

**Independent Test**: 掲示板を用意せず、POST /api/accountsの成功・無効入力・重複を確認する。登録UIの表示と項目エラーも独立して確認する。登録した情報でログインできることの結合確認はUS2完了後に行う。

### Tests for User Story 1

- [ ] T013 [P] [US1] `backend/src/test/java/com/gamerecruitment/account/domain/AccountInputTest.java`にIDの2/3/30/31文字、表示名の0/1/30/31コードポイント、パスワードの14/15/128/129コードポイント、Unicode空白のみ、大小と前後空白の保存方針のテストを作成する（Phase 2後）。
- [ ] T014 [P] [US1] `backend/src/test/java/com/gamerecruitment/account/contract/RegisterContractTest.java`にOpenAPIの201/400/403/409/415応答、未知のJSON項目、公開応答の秘密情報除外、登録時JWT未発行を検証するテストを作成する（Phase 2後）。

### Implementation for User Story 1

- [ ] T015 [US1] `backend/src/main/java/com/gamerecruitment/account/domain/Account.java`と`backend/src/main/java/com/gamerecruitment/account/domain/AccountInput.java`に「主キー。表示名やログインIDと独立した本人ID」「ASCII小文字英数字と_、3〜30文字」「1〜30コードポイント、Unicode空白のみ不可、重複可」、パスワード15〜128コードポイントの不変条件を実装する。IDだけASCII小文字化し、パスワードをtrim・正規化しない。
- [ ] T016 [US1] `backend/src/main/resources/db/migration/V1__create_accounts.sql`にid UUID「主キー」、login_id varchar(30)「NOT NULL、UNIQUE、ASCII小文字英数字と_、3〜30文字」、display_name varchar(30)「NOT NULL、1〜30コードポイント、Unicode空白のみ不可、重複可」、password_hash text「NOT NULL」、created_at timestamptz「NOT NULL」を反映する。空白判定はドメインでも保証する。
- [ ] T017 [P] [US1] `backend/src/main/java/com/gamerecruitment/account/adapter/persistence/JdbcAccountRepository.java`にJdbcClientによる保存・ID検索・本人ID検索と一意制約競合の変換を実装する（T015/T016後）。
- [ ] T018 [P] [US1] `backend/src/main/java/com/gamerecruitment/account/adapter/security/Argon2PasswordHasher.java`にArgon2id memory=19456KiB/iterations=2/parallelism=1/salt=16bytes/hash=32bytesを実装する。「Argon2idのsalt・パラメーター込みハッシュ」のみ保存し、Unicodeや72バイト超を切り捨てない（T015後、T017と並行可）。
- [ ] T019 [US1] `backend/src/main/java/com/gamerecruitment/account/application/RegisterAccount.java`と`backend/src/main/java/com/gamerecruitment/account/adapter/persistence/TransactionalRegistration.java`に登録ユースケースと外側のトランザクションを実装し、`backend/src/main/java/com/gamerecruitment/configuration/AccountConfiguration.java`で完成したAccountRepository・PasswordHasherと登録ユースケースを接続する。事前検索だけで一意性を判定せず、DB競合を409に変換する（T017/T018後）。
- [ ] T020 [US1] `backend/src/main/java/com/gamerecruitment/account/adapter/web/RegistrationController.java`で生成されたAPI interfaceを実装し、POST /api/accountsを提供する。OpenAPIから生成するRegisterRequestを内側の入力型へ変換し、処理結果を生成DTOのAccountへ変換して201でid/displayNameだけを返す。JSON形式とドメイン入力を検証する（T019後）。
- [ ] T021 [P] [US1] `frontend/src/features/account/api/register.ts`と`frontend/src/features/account/pages/RegisterPage.tsx`に契約に沿った入力・項目エラー・ID重複・登録成功を実装し、成功後は/loginへ案内する（T010/T015後、契約を使ってJava実装と並行可）。
- [ ] T022 [P] [US1] `frontend/src/features/account/pages/RegisterPage.test.tsx`に有効入力、文字数境界、空白のみ、409表示、パスワードを変形しないことのコンポーネントテストを作成する（T021後）。
- [ ] T023 [US1] `backend/src/test/java/com/gamerecruitment/account/integration/ConcurrentRegistrationTest.java`にTestcontainers PostgreSQLで同一ID/大小違いの10並列登録成功1件、同じ表示名の別ID成功、再接続で情報保持を検証し、T013/T014/T022とともに実行する（T020後）。

**Checkpoint**: 登録APIと画面が動作し、無効入力・競合が検証できる。US1だけではログインは未完成。

## Phase 4: User Story 2 - ログイン・ログアウトする (Priority: P1)

**Goal**: 1時間有効のログイン、本人確認、ブラウザ単位の失効を提供する。

**Independent Test**: 登録済みアカウントのfixtureを用意すれば登録画面を経由せず、認証とログアウトを検証できる。

### Tests for User Story 2

- [ ] T024 [P] [US2] `backend/src/test/java/com/gamerecruitment/account/security/JwtValidationTest.java`にHS256署名・iss/aud/sub/jti/iat/exp、必須claim欠落・型違い・改ざん、期限の3599秒/3600秒、操作で延長なし、失効JWT拒否を差し替えClockで検証するテストを作成する（Phase 2とUS1後）。
- [ ] T025 [P] [US2] `backend/src/test/java/com/gamerecruitment/account/contract/AuthenticationContractTest.java`にlogin/me/logoutの成功・401・409・CSRF403、無効Cookieが公開経路を妨げないこと、複数Set-Cookieと削除属性、no-storeの契約テストを作成する（Phase 2とUS1後）。

### Implementation for User Story 2

- [ ] T026 [US2] `backend/src/main/resources/db/migration/V2__create_revoked_tokens.sql`にjti UUID「主キー」、expires_at timestamptz「NOT NULL」、revoked_at timestamptz「NOT NULL」、期限削除用インデックスを設定する。JWT本体は保存しない。
- [ ] T027 [P] [US2] `backend/src/main/java/com/gamerecruitment/account/adapter/persistence/JdbcRevokedTokenStore.java`に冪等な失効登録・照会・期限後削除を実装する。重複jtiを失敗にしない（T026後）。
- [ ] T028 [P] [US2] `backend/src/main/java/com/gamerecruitment/account/adapter/security/JwtTokenService.java`にSpringのJWT機能によるHS256固定の発行・検証を実装する。32バイト以上の乱数鍵、iss=game-recruitment-board、aud=game-recruitment-api、UUID sub/jti、iatとexp=iat+3600、猶予0秒。非公開ID/表示名/パスワードをclaimへ入れない（T015後、T027と並行可）。
- [ ] T029 [US2] `backend/src/main/java/com/gamerecruitment/account/application/LoginAccount.java`に正規化IDによる認証、パスワード照合、存在しないIDのダミーハッシュ照合、固定期限発行を実装する。同じブラウザの有効ログイン中は409 ALREADY_AUTHENTICATEDにする（T017/T018/T028後）。
- [ ] T030 [US2] `backend/src/main/java/com/gamerecruitment/account/application/LogoutAccount.java`と`backend/src/main/java/com/gamerecruitment/account/adapter/persistence/TransactionalLogout.java`に失効登録・commit後成功の順序を実装する。再ログアウトは冪等、DB失敗は503、他端末のjtiを失効しない（T027/T028後）。
- [ ] T031 [US2] `backend/src/main/java/com/gamerecruitment/account/adapter/security/CookieTokenResolver.java`と`backend/src/main/java/com/gamerecruitment/account/adapter/security/CurrentAccountAuthenticator.java`にCookie専用JWT取得・検証、失効一覧照会と本人の存在確認を実装する。Authorization/URLからJWTを受け付けず、DB障害時に認証を通さない（T027/T028後）。
- [ ] T032 [US2] `backend/src/main/java/com/gamerecruitment/account/adapter/web/AuthenticationController.java`と`backend/src/main/java/com/gamerecruitment/account/adapter/security/AuthCookieWriter.java`に生成されたAPI interfaceとHTTP DTOを使ってlogin/me/logoutを実装する。`backend/src/main/java/com/gamerecruitment/configuration/AccountConfiguration.java`で完成したTokenIssuer・RevokedTokenStoreと認証ユースケースを接続する。auth_tokenはHttpOnly、Path=/api、SameSite=Lax、Domainなし、Max-Age=3600。通常Secure、local loopbackのみ例外。login/logout成功後にCSRFをクリアし、不正・期限切れ・CookieなしのlogoutはCSRF検証後204にする（T029〜T031後）。
- [ ] T033 [US2] `backend/src/main/java/com/gamerecruitment/configuration/SecurityConfiguration.java`へT031の認証を組み込み、logoutを通常のJWT401拒否で遮断しない。全POSTのCSRF/Originを維持し、JSONのセキュリティエラーを契約とそろえる（T032後）。
- [ ] T034 [P] [US2] `backend/src/main/java/com/gamerecruitment/account/adapter/persistence/RevokedTokenCleanup.java`に1時間ごとの期限切れ記録整理を実装する。削除ジョブ停止でも期限検証が成立する構成を保つ（T027後、controllerと並行可）。
- [ ] T035 [US2] `frontend/src/features/account/api/auth.ts`と`frontend/src/features/account/AuthProvider.tsx`に初期csrf/me、本人・固定期限表示、401の再ログイン案内、ログイン/ログアウト後csrf再取得、503時の成功誤表示防止を実装する（T010/T032後）。
- [ ] T036 [US2] `frontend/src/features/account/pages/LoginPage.tsx`、`frontend/src/features/account/components/AccountStatus.tsx`、`frontend/src/App.tsx`にログイン画面、ログアウト操作、登録画面への導線を実装する。JWTはJSから読まず、他タブは次の認証確認で状態を更新する（T021/T035後）。
- [ ] T037 [P] [US2] `frontend/src/features/account/AuthProvider.test.tsx`と`frontend/src/features/account/pages/LoginPage.test.tsx`に401/403/409/503、CSRF再取得、期限表示、POSTの自動再送なしを検証するテストを作成する（T035/T036後）。
- [ ] T038 [US2] `backend/src/test/java/com/gamerecruitment/account/integration/LogoutRevocationTest.java`にコピーしたJWTの再利用拒否、別端末維持、失効commit失敗503、再起動で失効保持、期限切れ整理を実DBで検証し、T024/T025/T037を実行する（T033/T034後）。

**Checkpoint**: 登録〜ログイン〜本人確認〜ログアウトがつながり、US1/US2を独立したfixtureでも検証できる。

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: 全体の契約適合と学習用手順を仕上げる。

- [ ] T039 `frontend/playwright.config.ts`と`frontend/tests/auth.spec.ts`にブラウザ結合テストを作成・実行し、登録後明示ログイン、同一Cookieの複数タブ、異なるコンテキストでの別端末ログイン、ログアウト後の再利用拒否と再ログインを確認する。
- [ ] T040 `tests/contract/pyproject.toml`、`tests/contract/uv.lock`、`tests/contract/conftest.py`、`tests/contract/test_auth_contract.py`にSchemathesis＋pytestの契約テストを実装する。`schema/auth/openapi.yaml`を読み込み、起動したテスト用APIの5エンドポイントの応答コード・Content-Type・ヘッダー・本文を照合する。ケースごとにCSRFとCookieを準備し、登録/ログイン成功とlogout後の再準備を確認する。認証失敗だけのテストで合格とせず、正常系と異常系を検証する。固定seed・実行コマンド・秘密値を除いた結果を`specs/001-auth/quickstart.md`へ記録し、Java verifyとは別の契約検証コマンドとして実行する。frontendのtypecheck/buildとT011も確認する。
- [ ] T041 `backend/src/test/resources/fixtures/`と`specs/001-auth/quickstart.md`にテスト環境限定のアカウント・失効データ初期化手順を用意し、同一IDで検証を2回繰り返せることを確認する。通常利用者向けAPIを追加せず、掲示板実装後の関連募集/参加も含めた全体初期化を引き継ぎ事項として記録する。
- [ ] T042 `specs/001-auth/quickstart.md`に実装したコマンド、修正版の固定値、起動/停止/volume保持、実行結果を反映し、specの登録〜ログイン2分と初見5人中4人の目標を実測または未実施として正確に記録する。手動検証者が必要な場合は作業状況を示して依頼する。

## Dependencies & Execution Order

### Phase Dependencies

```text
Phase 1（T001〜T005）
  ↓
Phase 2（T006〜T012）
  ↓
US1（T013〜T023）：登録
  ↓
US2（T024〜T038）：ログイン・ログアウト
  ↓
Phase 5（T039〜T042）：結合検証・手順
```

### User Story Dependencies

- US1の保存モデル・ハッシュ境界をUS2が利用するため、実装順はUS1→US2とする。
- US2の独立検証は登録済みfixtureで行え、登録画面に依存しない。
- US1の登録入力・競合・公開応答はログイン実装前に検証できる。仕様の「登録情報でログインできる」はUS2後の結合テストで確認する。
- 掲示板機能は本人ID・公開表示名・認証契約を再利用する。募集ごとの権限は掲示板タスクで扱う。

### Within Each User Story

- 先に期待する振る舞いのテストを定義し、モデル→保存/暗号アダプター→ユースケース→HTTP/画面→結合検証の順で実装する。
- 各[P]タスクの先行条件を守る。並行可能とは、その先行条件が未完了でも実行できる意味ではない。
- 失敗や新たな仕様不足が見つかった場合は、先に仕様・設計・契約を更新する。

### Parallel Opportunities

- T001後: T002/T003/T004は異なる設定ファイルで並行できる。
- T006後: T007/T008/T010/T011は独立した基盤ファイルで並行できる。
- US1: T013/T014は別テスト、T017/T018はSQLとハッシュの別アダプター。
- US2: T024/T025は別テスト、T027/T028は失効保存とJWT、T034は既存失効境界を使った定期処理。
- 同じSecurityConfigurationやpom.xmlを変更する作業は並行させない。

## Parallel Example: User Story 1

T015/T016完了後、T017のJdbcAccountRepositoryとT018のArgon2PasswordHasherを並行作成できる。
両方が完了してからT019の登録ユースケースへ進む。

## Parallel Example: User Story 2

T026完了後、T027のJdbcRevokedTokenStoreとT028のJwtTokenServiceを並行作成できる。
両方をT030/T031へ接続し、T032/T033のHTTP・認証設定は順に統合する。

## Implementation Strategy

### MVP First

US1だけではログインを使えないため、最初の利用可能なアカウント機能はUS1＋US2とする。
途中でUS1を登録API/画面のデモとして確認し、認証までの到達を最初の完成目標とする。

### Incremental Delivery

1. 開発環境と共通基盤を整える。
2. 登録入力・保存・画面を実装して検証する。
3. ログイン・JWT・失効・画面を実装して検証する。
4. 両ストーリーの結合と契約照合を行い、利用者向けの起動手順を確定する。
5. アカウント機能を基に掲示板の設計・タスクへ進む。

## Notes

- 42タスク。US1は11、US2は15、Setupは5、Foundationalは7、横断は4。
- ブランチ変更・切り替えは、ユーザーの指定どおり事前確認する。現在のタスク生成はplan-accountで行う。
- 作業の区切りでコミット候補をまとめる。コミット/プッシュ・公開は実装タスク完了の代替にしない。
- 検証目標に手動参加者が必要な場合は、実施できない目標を合格扱いにしない。
