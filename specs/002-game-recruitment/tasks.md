# Tasks: ゲーム募集掲示板 初版

**Input**: `specs/002-game-recruitment/`の設計資料と`schema/game-recruitment/openapi.yaml`

**Prerequisites**: [plan.md](plan.md)、[spec.md](spec.md)、[research.md](research.md)、[data-model.md](data-model.md)、[契約ガイド](contracts/README.md)、[OpenAPI](../../schema/game-recruitment/openapi.yaml)、[quickstart.md](quickstart.md)

**Tests**: 仕様のAcceptance Scenarios、Edge Cases、SC-004/005/007を自動検証する。API契約はSchemathesis、APIレベルのユーザーシナリオはHurl、画面操作はPlaywright、競合とトランザクションはJava/Testcontainersで分担する。

**Organization**: US1〜US7を仕様の優先順で実装する。各ストーリーのテストを先に作り、失敗を確認してから実装する。

## Format: `[ID] [P?] [Story] Description`

- `[P]`: 記載した先行条件を満たし、異なるファイルを変更するため並行可能。
- `[USn]`: `spec.md`のUser Story nに対応。Setup、Foundational、横断作業には付けない。
- 完了した作業だけを`[X]`にする。

## Path Conventions

- Java本体: `backend/src/main/java/com/gamerecruitment/`
- Javaテスト: `backend/src/test/java/com/gamerecruitment/`
- DB migration: `backend/src/main/resources/db/migration/`
- React本体: `frontend/src/`、ブラウザテスト: `frontend/tests/`
- API契約: `schema/game-recruitment/openapi.yaml`
- Schemathesis: `tests/contract/`、Hurl: `tests/scenarios/`
- 現在の設計ブランチは`plan-game-recruitment`。実装前に別ブランチへ切り替える場合はユーザーへ事前確認する。

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: 既存authを壊さず、掲示板契約・Hurl・DB変更を追加できる実装基盤を用意する。

- [X] T001 `backend/pom.xml`へ`schema/game-recruitment/openapi.yaml`を検証・生成するOpenAPI Generator 7.26.0の第2executionを追加し、`backend/target/generated-sources/game-recruitment/`と`com.gamerecruitment.recruitment.adapter.web.generated.api/model`を使用する。authと同じ`interfaceOnly=true`、`skipDefaultInterface=true`、`useSpringBoot4=true`、`useJackson3=true`、`useBeanValidation=false`、`skipValidateSpec=false`を維持し、既存auth生成物を上書きしない。
- [X] T002 [P] `frontend/package.json`と`frontend/package-lock.json`へ公式安定版`@orangeopensource/hurl`をdevDependencyとして固定し、`test:scenario` scriptを追加してNode.js 22で`hurl --version`と最小の構文確認を行う。インストールやネットワーク許可が必要なら承認を求め、実際の固定版を`specs/002-game-recruitment/quickstart.md`へ記録する。
- [X] T003 [P] `backend/src/main/resources/db/migration/V3__create_recruitments.sql`に`id UUID PRIMARY KEY`、`organizer_id UUID NOT NULL REFERENCES accounts(id) ON DELETE RESTRICT`、3ゲームだけの`game varchar(20)`、`title varchar(100)`、`description varchar(2000)`、`starts_at timestamptz`、`capacity smallint CHECK 1〜9`、`meeting_info varchar(1000) NOT NULL DEFAULT ''`、`manually_closed/cancelled boolean NOT NULL DEFAULT false`、`created_at timestamptz`と計画の3索引を実装する。
- [X] T004 [P] `backend/src/main/resources/db/migration/V4__create_participations.sql`に`recruitment_id`と`account_id`の複合主キー、両FKの`ON DELETE RESTRICT`、`joined_at timestamptz NOT NULL`、`(account_id,recruitment_id)`索引を実装する。
- [X] T005 `backend/src/test/resources/fixtures/reset_contract.sql`、`backend/src/test/java/com/gamerecruitment/account/ApiTestSupport.java`、`scripts/reset-contract-db.sh`の専用DB初期化を`participations,recruitments,revoked_tokens,accounts`の明示的な一括TRUNCATEへ拡張し、DB名guardを維持して開発DBと固定ゲーム値を変更しない。

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: 全ストーリーが共有するdomain、依存境界、認証、エラー、トランザクションを完成させる。

**⚠️ CRITICAL**: このPhaseが完了するまで個別ストーリーの実装へ進まない。

- [X] T006 `backend/src/main/java/com/gamerecruitment/recruitment/domain/Game.java`、`RecruitmentState.java`、`RecruitmentInput.java`、`Recruitment.java`に3ゲーム固定値、中止→開始済み→手動締切→満員→受付中の状態優先順、Unicodeコードポイント、White_Spaceのみ拒否、不正surrogate拒否、title 1〜100、description 1〜2000、capacity整数1〜9、meetingInfo最大1000と空白のみ未登録、offset付き日時の秒/小数秒0を実装する。入力文字のtrim・大小変換・Unicode正規化は行わない。
- [X] T007 [P] `backend/src/test/java/com/gamerecruitment/recruitment/domain/RecruitmentInputTest.java`と`RecruitmentStateTest.java`に各文字数の最小/最大/範囲外、改行・絵文字・全Unicode White_Space、0/1/9/10、3ゲーム/未知値、状態優先順、開始直前/ちょうど/直後の失敗テストを先に作る。
- [X] T008 `backend/src/main/java/com/gamerecruitment/recruitment/application/port/RecruitmentRepository.java`と`RecruitmentQueries.java`に更新用snapshot/親行lock/参加関係操作と、公開一覧・公開詳細・本人別一覧のquery契約を定義する。内側の型へSpring、JDBC、SQL、生成DTO、HTTP型を公開しない。
- [X] T009 `backend/src/main/java/com/gamerecruitment/recruitment/application/RecruitmentFailure.java`と`backend/src/main/java/com/gamerecruitment/recruitment/adapter/web/RecruitmentExceptionHandler.java`を作り、掲示板専用code/fieldsを400/403/404/409へ変換する。`backend/src/main/java/com/gamerecruitment/shared/adapter/web/ApiErrorBody.java`へString code/message/fieldsの共通envelopeを置き、auth生成enumに掲示板値を渡さず既存auth契約を維持する。
- [X] T010 `backend/src/main/java/com/gamerecruitment/account/adapter/security/CurrentAccountFilter.java`と`backend/src/main/java/com/gamerecruitment/configuration/SecurityConfiguration.java`をHTTPメソッド・URL単位で拡張し、`GET /api/games`と公開一覧/詳細は不正Cookieでも閲覧可能、合流情報GET・本人一覧GET・全掲示板POSTは既存JWT/失効/本人存在の認証必須、全POSTはCSRF/Origin必須とする。CSRF欠落は403、正しいCSRFで未認証は401、DB障害は503、全応答no-storeを維持する。
- [X] T011 [P] `backend/src/test/java/com/gamerecruitment/recruitment/contract/RecruitmentSecurityContractTest.java`に公開GETのCookieなし/不正Cookie成功、private GET/POSTの401、CSRF/Originの403、失効照会503、auth APIの応答回帰、掲示板code/fieldsが500にならないことを先に実装する。
- [X] T012 `backend/src/main/java/com/gamerecruitment/recruitment/adapter/persistence/JdbcRecruitmentRepository.java`と`TransactionalRecruitmentCommands.java`にREAD COMMITTEDのTransactionTemplate、全変更で親行を最初に`SELECT ... FOR UPDATE`、lock後の別SQLで人数/本人参加を再読込、Clockのlock後/終了直前再評価、commit後成功を実装する。lock SQLへCOUNT/EXISTSを混ぜず、全経路を親行→参加行の順にする。
- [X] T013 `backend/src/main/java/com/gamerecruitment/configuration/RecruitmentConfiguration.java`で既存Clock、repository/query、command usecase、transaction decoratorを配線し、`backend/src/test/java/com/gamerecruitment/architecture/DependencyTest.java`へrecruitment domain/applicationからSpring/JDBC/adapter/configuration/generated型への依存禁止を追加する。

**Checkpoint**: 生成、migration、domain、認証、共通エラー、更新transactionが利用可能。

---

## Phase 3: User Story 1 - 遊びたいゲームの募集を探す (Priority: P1) 🎯 MVP

**Goal**: 未ログインで固定ゲーム、受付中一覧、公開詳細を確認できる。

**Independent Test**: 状態・ゲーム・日時が異なるfixtureを用意し、Cookieなしで受付中だけの順序・絞り込み・20件ページング・公開詳細・空表示を確認する。

### Tests for User Story 1

- [X] T014 [P] [US1] `backend/src/test/java/com/gamerecruitment/recruitment/contract/PublicRecruitmentContractTest.java`にGET games/list/detailの200、game/page/UUID不正400、存在しない有効UUID404、20件＋hasNext、公開JSONにmeetingInfo/loginId/password/JWTが存在しないことを先に作る。
- [ ] T015 [P] [US1] `backend/src/test/java/com/gamerecruitment/recruitment/integration/PublicRecruitmentQueryTest.java`にOPENのみ、startsAt/createdAt/id昇順、3ゲーム絞り込み、detailのparticipantCountとparticipants配列長一致、同時更新時も単一snapshotで矛盾しないことを実PostgreSQLで先に作る。
- [ ] T016 [P] [US1] `frontend/src/features/recruitment/pages/RecruitmentListPage.test.tsx`と`RecruitmentDetailPage.test.tsx`に一覧、ゲーム絞り込み、次ページ、空、404、状態/参加者の表示、offset付き日時をAsia/Tokyoで表示する日付境界、外部JSONの未知/欠落/型違い拒否を先に作る。

### Implementation for User Story 1

- [X] T017 [US1] `backend/src/main/java/com/gamerecruitment/recruitment/adapter/persistence/JdbcRecruitmentQueries.java`に固定ゲーム、OPEN条件（cancelled=false、manual=false、startsAt>Clock、count<capacity）、20+1件、決定的順序、単一SQLの公開詳細/profile/参加者/countを実装する。
- [X] T018 [US1] `backend/src/main/java/com/gamerecruitment/recruitment/adapter/web/PublicRecruitmentController.java`に生成`GamesApi`/`RecruitmentsApi`の公開GETを実装し、pageは1始まり、固定20、overflow防止、合流情報なしのDTO変換を行う。
- [ ] T019 [P] [US1] `frontend/src/features/recruitment/api/validation.ts`と`api/recruitments.ts`にGame、RecruitmentSummary/Detail/Pageのstrictな実行時検証と公開GET clientを実装する。
- [ ] T020 [US1] `frontend/src/features/recruitment/pages/RecruitmentListPage.tsx`、`RecruitmentDetailPage.tsx`、`frontend/src/App.tsx`に`/`一覧、game選択、ページ移動、`/recruitments/:id`公開詳細、空/404/エラー表示を追加し、開始日時をAsia/Tokyoで、本文/表示名をプレーンテキストで表示する。

**Checkpoint**: US1だけで未ログイン利用者が募集を検索・閲覧できる。

---

## Phase 4: User Story 2 - 仲間を募集する (Priority: P1)

**Goal**: ログイン利用者が有効な募集を作成し、自分の作成募集へ残せる基礎を提供する。

**Independent Test**: ログインfixtureから募集を作成し、0人の公開詳細とDB保存を確認する。境界外、現在以前、待機後に開始時刻へ達した要求はrollbackする。

### Tests for User Story 2

- [ ] T021 [P] [US2] `backend/src/test/java/com/gamerecruitment/recruitment/contract/CreateRecruitmentContractTest.java`に201＋Location、0人、公開detail、400/401/403/415/503、未知JSON、capacity小数/文字列、offsetなし/秒あり/不実在/現在以前、meetingInfo非公開、PUT/PATCH/DELETE 405を先に作る。
- [ ] T022 [P] [US2] `backend/src/test/java/com/gamerecruitment/recruitment/integration/CreateRecruitmentTransactionTest.java`にlock/最終判定までにClockがstartsAtへ達したrollback、commit失敗503、DB再接続後保持、表示名重複でもorganizer UUIDを使うことを先に作る。
- [ ] T023 [P] [US2] `frontend/src/features/recruitment/pages/CreateRecruitmentPage.test.tsx`に3ゲーム、title 1/100、description 1/2000、meetingInfo 0/1000、Unicode/改行保持、capacity 1/9、JST分入力、入力項目エラー、作成成功導線を先に作る。

### Implementation for User Story 2

- [ ] T024 [US2] `backend/src/main/java/com/gamerecruitment/recruitment/application/CreateRecruitment.java`にJWT本人UUID、入力条件、Clockによる未来性、参加0、両flag false、meetingInfo空白未登録、最終時刻再確認を実装し、`JdbcRecruitmentRepository`のINSERTへ接続する。
- [ ] T025 [US2] `backend/src/main/java/com/gamerecruitment/recruitment/adapter/web/RecruitmentCommandController.java`にPOST /recruitmentsを実装し、生成requestを内側へ変換、201/Location/公開detailをcommit後に返す。organizerIdをrequestから受け取らない。
- [ ] T026 [P] [US2] `frontend/src/features/recruitment/input.ts`と`api/commands.ts`にコードポイント/White_Space、capacity整数、JSTの`datetime-local`へ`+09:00`を明示する変換、POSTの実行時応答検証を実装する。
- [ ] T027 [US2] `frontend/src/features/recruitment/pages/CreateRecruitmentPage.tsx`と`frontend/src/App.tsx`にログイン必須の作成画面、textareaによる改行保持、修正可能な項目エラー、成功後の詳細/マイページ導線を実装する。

**Checkpoint**: US2で募集作成と公開確認が独立して動作する。

---

## Phase 5: User Story 3 - 募集に参加して合流情報を確認する (Priority: P1)

**Goal**: 別のログイン利用者がOPEN募集へ即時参加し、開始前・中止前の合流情報を見られる。

**Independent Test**: 募集者/参加者fixtureで参加→人数/マイページ反映→合流情報閲覧を確認し、募集者・重複・満員・非OPEN・未認証・同時最後1枠を拒否する。

### Tests for User Story 3

- [ ] T028 [P] [US3] `backend/src/test/java/com/gamerecruitment/recruitment/contract/JoinAndMeetingInfoContractTest.java`に参加201、合流情報200/未登録表示、401/403/404/409、OWN_RECRUITMENT、ALREADY_PARTICIPATING、FULL/CLOSED/CANCELLED/STARTED、公開JSONの非漏えいを先に作る。
- [ ] T029 [US3] `backend/src/test/java/com/gamerecruitment/recruitment/integration/ConcurrentParticipationTest.java`に残り1枠へ10人同時参加で成功1、同一人同時参加1行、親lock待機中にClockが開始時刻へ達した要求の拒否、DB人数/公開count/配列一致を先に作る。
- [ ] T030 [P] [US3] `frontend/src/features/recruitment/components/ParticipationActions.test.tsx`と`MeetingInfoPanel.test.tsx`に参加成功、各409理由、未登録表示、権限403、401、503時の成功誤表示なしを先に作る。

### Implementation for User Story 3

- [ ] T031 [US3] `backend/src/main/java/com/gamerecruitment/recruitment/application/JoinRecruitment.java`に募集者以外・未参加・OPEN・空きありを親lock後/最終Clockで確認して参加行INSERTする処理を実装し、複合PK競合もALREADY_PARTICIPATINGへ変換する。
- [ ] T032 [US3] `backend/src/main/java/com/gamerecruitment/recruitment/application/GetMeetingInfo.java`とpersistence adapterにreadOnly=falseの短期transaction、親行FOR SHARE、本人参加の別SQL、募集者全状態可/参加者は開始前かつ中止前のみ/その他403を実装する。
- [ ] T033 [US3] `RecruitmentCommandController.java`へPOST participation/meを、`backend/src/main/java/com/gamerecruitment/recruitment/adapter/web/MeetingInfoController.java`へprivate GETを実装し、requestからaccountIdを受け取らず、commit後の公開detailまたはregistered/textだけを返す。
- [ ] T034 [US3] `frontend/src/features/recruitment/components/ParticipationActions.tsx`、`MeetingInfoPanel.tsx`、`RecruitmentDetailPage.tsx`に参加操作、最新detail反映、権限別の合流情報取得、ログアウト/401/期限到達時のprivate state削除を実装する。

**Checkpoint**: P1の検索・作成・参加・合流準備が一続きで動作する。

---

## Phase 6: User Story 4 - 参加をキャンセルする (Priority: P2)

**Goal**: 参加者本人が開始前に参加を取り消し、空きと権限を正しく戻す。

**Independent Test**: OPEN/FULL/manual/cancelledの開始前参加を取り消し、人数、状態、本人一覧、合流情報権限、再参加を確認する。

### Tests for User Story 4

- [ ] T035 [P] [US4] `backend/src/test/java/com/gamerecruitment/recruitment/integration/CancelParticipationTest.java`にFULL→OPEN、manual/cancelled維持、開始ちょうど拒否、他人/未参加/二重キャンセル拒否、同時二重取消で1行だけDELETE、取消とcloseの競合でmanual維持、再参加を先に作る。
- [ ] T036 [P] [US4] `frontend/src/features/recruitment/components/ParticipationActions.test.tsx`へキャンセル後の人数/状態、合流情報state削除、本人一覧から除外、409/503時に成功扱いしないテストを追加する。

### Implementation for User Story 4

- [ ] T037 [US4] `backend/src/main/java/com/gamerecruitment/recruitment/application/CancelParticipation.java`に親lock、本人参加、開始前を確認して本人行だけDELETEし、manual/cancelled flagを変更せず、最終Clockを再確認する処理を実装する。
- [ ] T038 [US4] `RecruitmentCommandController.java`へPOST participations/me/cancelを実装し、commit後の公開detailを返す。`ParticipationActions.tsx`と本人一覧stateへキャンセル、再参加、private情報破棄を接続する。

**Checkpoint**: US4を追加しても定員・手動締切・中止の状態が矛盾しない。

---

## Phase 7: User Story 5 - 募集を管理する (Priority: P2)

**Goal**: 募集者が開始前に締切・再開・中止・合流情報更新を行える。

**Independent Test**: 本人/他人と全状態のfixtureで、状態遷移と仕様の合流情報権限表を網羅する。

### Tests for User Story 5

- [ ] T039 [P] [US5] `backend/src/test/java/com/gamerecruitment/recruitment/domain/RecruitmentPermissionsTest.java`にclose/reopen/cancel/update/viewの本人・参加者・未参加、OPEN/FULL/manual/cancelled/started、開始直前/ちょうど/直後の権限表を先に作る。
- [ ] T040 [US5] `backend/src/test/java/com/gamerecruitment/recruitment/integration/RecruitmentManagementTest.java`にFULLからmanual close、空きありreopen、cancel不可逆、参加履歴維持、cancel対meeting更新、reopen対join、lock待機後開始時刻、commit失敗rollbackを先に作る。
- [ ] T041 [P] [US5] `frontend/src/features/recruitment/components/RecruitmentManagement.test.tsx`と`MeetingInfoEditor.test.tsx`に表示権限、成功後状態、text 0/1000/1001/空白未登録、NOT_OWNER/各409/503を先に作る。

### Implementation for User Story 5

- [ ] T042 [US5] `backend/src/main/java/com/gamerecruitment/recruitment/application/ManageRecruitment.java`にclose（FULL可、既manualは成功）、reopen（manualかつ空き）、cancel（開始前/中止前・不可逆）、updateMeetingInfo（開始前/中止前、FULL/manual可、meetingInfoだけ）を親lock/最終Clockで実装する。
- [ ] T043 [US5] `RecruitmentCommandController.java`へclose/reopen/cancelを、`MeetingInfoController.java`へPOST meeting-infoを実装し、募集者UUIDをJWTから取り、NOT_OWNER 403と状態409を契約どおり返す。
- [ ] T044 [US5] `frontend/src/features/recruitment/components/RecruitmentManagement.tsx`、`MeetingInfoEditor.tsx`、`RecruitmentDetailPage.tsx`に募集者だけの操作、確認表示、更新後の最新detail、参加者の中止表示を実装する。

**Checkpoint**: 状態遷移と合流情報の閲覧・編集権限が仕様表と一致する。

---

## Phase 8: User Story 6 - 自分の募集と参加予定を確認する (Priority: P2)

**Goal**: 自分の作成/参加募集を開始前/過去の4区分で確認できる。

**Independent Test**: 状態と日時が異なる自分/他人の募集を用意し、4区分・順序・ページ・空・キャンセル除外・再ログイン保持を確認する。

### Tests for User Story 6

- [ ] T045 [P] [US6] `backend/src/test/java/com/gamerecruitment/recruitment/integration/MyRecruitmentsQueryTest.java`にcreated/participating、upcoming/past、各20件＋hasNext、未来昇順/過去降順、中止の日時区分、満員/manual保持、他人除外、キャンセル除外、DB再接続保持を先に作る。
- [ ] T046 [P] [US6] `frontend/src/features/recruitment/pages/MyRecruitmentsPage.test.tsx`に4区分、個別ページ移動、空、状態、offset付き日時のAsia/Tokyo表示と開始前/過去の日付境界、401、再表示、キャンセル/中止後反映を先に作る。

### Implementation for User Story 6

- [ ] T047 [US6] `JdbcRecruitmentQueries.java`にJWT本人UUIDを使うcreated/participatingとstartsAt>now/upcoming・startsAt<=now/past、各20+1件、未来昇順/過去降順を実装し、`backend/src/main/java/com/gamerecruitment/recruitment/adapter/web/MyRecruitmentsController.java`へGET /me/recruitmentsを追加する。
- [ ] T048 [US6] `frontend/src/features/recruitment/pages/MyRecruitmentsPage.tsx`、`api/recruitments.ts`、`frontend/src/App.tsx`に`/mypage`の4区分、開始日時のAsia/Tokyo表示、区分ごとのpage/hasNext、空表示、詳細/管理導線、401時ログイン案内を実装する。

**Checkpoint**: US6で一覧に出ない募集も本人が確認・管理できる。

---

## Phase 9: User Story 7 - テスト用データを初期化する (Priority: P3)

**Goal**: 専用テスト環境だけを安全に初期化し、同じIDで全工程を繰り返せる。

**Independent Test**: アカウント・募集・参加・失効を作成して初期化し、孤児データなし、同一IDで2回、通常APIに削除機能なしを確認する。

### Tests and Implementation for User Story 7

- [ ] T049 [US7] `backend/src/test/resources/fixtures/reset_contract.sql`と`scripts/reset-contract-db.sh`に対し、contract DBでは全4表が空になり固定ゲームGETは残ること、別DB名ではguardが拒否すること、通常利用者向け初期化/退会/募集DELETE APIがOpenAPIにもroutingにもないことを`backend/src/test/java/com/gamerecruitment/recruitment/integration/ContractResetTest.java`で検証する。
- [ ] T050 [US7] `tests/contract/test_recruitment_contract.py`と`tests/contract/conftest.py`に12操作のSchemathesis 5 checks、freshな複数本人/CSRF/未来募集fixture、正常系を含む明示400/401/403/404/409/415と固定seedを実装し、同じIDで登録→募集→参加→resetを2回確認する。

**Checkpoint**: 反復可能な検証環境が完成し、開発DBは保護される。

---

## Phase 10: Polish & Cross-Cutting Concerns

**Purpose**: Hurlの最終ユーザーシナリオ、画面結合、回帰、手順を完成させる。

- [ ] T051 `tests/scenarios/recruitment-lifecycle.hurl`に2アカウント登録・ログイン、募集作成とID capture、未ログイン検索/公開非漏えい、参加、人数/マイページ/合流情報、キャンセル、403、ログアウト後401を`--no-cookie-store`と役割別Cookie/CSRF captureで実装する。
- [ ] T052 `tests/scenarios/recruitment-management.hurl`と`errors-and-access.hurl`に合流情報更新、締切、キャンセル後の再開、中止、各状態/権限、未認証、他人操作、状態409を実装し、status、no-store、主要JSONをassertする。JWT/パスワード/CSRF値をファイル・レポート・共有ログへ保存しない。
- [ ] T053 `scripts/run-hurl-scenarios.sh`と`tests/scenarios/README.md`にcontract DB初期化、衝突しないID、未来JST分日時、base_urlの変数生成、Hurlの直列実行、失敗時の秘密非表示、固定版と学習用のcapture/assert説明を実装する。
- [ ] T054 `frontend/tests/recruitment.spec.ts`にReact画面で検索→登録/ログイン→作成→別context参加→合流情報→キャンセル→管理→マイページを実装し、既存`frontend/tests/auth.spec.ts`も実行してJWT/CSRF/複数タブ回帰を確認する。
- [ ] T055 T051〜T054の完了後、`backend/src/test/java/com/gamerecruitment/architecture/DependencyTest.java`と全Javaテスト、frontend typecheck/Vitest/build、OpenAPI Generator、Schemathesis、Hurl、Playwrightを最終版で実行し、失敗を修正して結果を`specs/002-game-recruitment/quickstart.md`と`contracts/README.md`へ秘密値なしで記録する。
- [ ] T056 `specs/002-game-recruitment/quickstart.md`とルート`README.md`に起動/停止、migration、Docker volume、contract DB、Schemathesis/Hurl/Playwrightの役割とコマンド、Hurlのcapture/assert、実行結果、未実施の手動目標を反映する。SC-001/002/003/006は手動で実測した場合だけ達成と記録し、未実施を合格扱いしない。

---

## Dependencies & Execution Order

### Phase Dependencies

```text
Phase 1 Setup (T001〜T005)
  ↓
Phase 2 Foundation (T006〜T013)
  ↓
US1 公開検索 (T014〜T020)
  ↓
US2 作成 (T021〜T027)
  ↓
US3 参加・合流情報 (T028〜T034)
  ↓
US4 キャンセル (T035〜T038) ─┐
US5 募集管理 (T039〜T044) ───┼→ US6 マイページ (T045〜T048)
                               ↓
US7 初期化 (T049〜T050)
  ↓
Polish/Hurl最終シナリオ (T051〜T056)
```

### User Story Dependencies

- US1はFoundation後に単独で公開検索MVPとして完成できる。
- US2は共通domain/repositoryを使い、作成後の確認にUS1の公開詳細を使う。
- US3はUS2の募集fixtureとUS1のdetailを使う。ここまでがP1の利用可能範囲。
- US4とUS5はUS3の参加関係を前提にするが、互いの実装ファイルが重なるため順に統合する。
- US6はUS2/US3/US4/US5の全状態を表示するため、その後に実装する。
- US7は全関連表と全工程を初期化するため全機能後。
- Hurlは最終ユーザーシナリオなので、全APIとreset完成後に実装・実行する。

### Within Each User Story

- 仕様に対応するテストを先に作り、未実装理由で失敗することを確認する。
- domain/port → persistence/usecase → controller → frontend API →画面の順に実装する。
- 親行lockを使う変更は同じrepository/transactionファイルで順番に統合し、並行編集しない。
- 失敗でDBや画面stateを成功扱いにせず、commit完了後だけ成功応答を返す。

### Parallel Opportunities

- T002/T003/T004はT001と独立したファイルで並行可能。T003/T004後にT005を行う。
- FoundationではT007とT011のテスト準備を、対応する本体の前に異なるファイルで進められる。
- 各USのJava contract/integrationテストとReact componentテストは別ファイルで並行可能。
- US4/US5は業務的には分離できるが、同じcommand/controller/repositoryを変更するため実装は順次統合する。
- T051/T052はHurl別ファイルだが共通fixture設計を先に合意し、T053で直列実行する。

## Parallel Examples

### User Story 1

```text
T014 PublicRecruitmentContractTest
T015 PublicRecruitmentQueryTest
T016 React list/detail tests
```

### User Story 3

```text
T028 JoinAndMeetingInfoContractTest
T030 React participation/meeting tests
```

T029の同時参加テストは共有DBとlock制御を使うため単独で実行する。

## Implementation Strategy

### MVP First

1. Phase 1と2を完了する。
2. US1を実装し、未ログインでゲーム絞り込み・一覧・詳細を確認する。
3. US2、US3を追加して「募集を作り、別利用者が参加して合流情報を見る」P1フローを完成する。
4. P1 checkpointでJava・React・契約を実行する。

### Incremental Delivery

1. 公開検索。
2. 募集作成。
3. 即時参加・合流情報。
4. 参加キャンセルと募集管理。
5. マイページと安全な初期化。
6. Schemathesisで契約全体、Hurlで最終ユーザーシナリオ、Playwrightで画面シナリオを確認する。

## Notes

- 全56タスク。Setup 5、Foundation 8、US1 7、US2 7、US3 7、US4 4、US5 6、US6 4、US7 2、横断6。
- Hurlは最後のユーザーシナリオテストとしてT051〜T053に配置し、早い段階のT002で再現可能な固定版と実行入口だけ用意する。
- OpenAPI、Schemathesis、Hurl、Playwright、Javaの役割を混同しない。
- 仕様変更が必要な場合は、実装より先にspec、plan、OpenAPI、関連タスクを更新する。
- 作業の区切りでコミット候補をまとめる。コミット/プッシュはタスク完了の代替にしない。
