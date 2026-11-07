# Implementation Plan: アカウント 初版

**Branch**: `plan-account` | **Date**: 2026-10-03 | **Spec**: [spec.md](spec.md)

**Input**: `specs/001-auth/spec.md` と [共通設計方針](../002-game-recruitment/api-design-decision.md)

## Summary

登録・ログイン・ログアウト・現在の本人確認をSpring Boot APIとReact画面で提供する。
ログインごとに1時間有効なJWTをHttpOnly Cookieへ設定し、PostgreSQLの失効一覧でログアウトを保証する。
登録条件、複数端末、期限境界を契約とテストに対応付ける。実装進捗はtasks.md、実行結果はquickstartと契約ガイドで管理する。

## Technical Context

**Language/Version**: Java 25（Temurin、previewなし）、TypeScript 5.9（strict）。

**Primary Dependencies**: Spring Boot 4.1.1、Spring MVC/Security/JDBC、OAuth2 Resource Server/JOSE、Flyway、Argon2id/Bouncy Castle。React 19.3、Vite 8、npm。Javaの管理対象依存はBoot BOMに従う。Maven Wrapper 3.10.0。

**Storage**: PostgreSQL 18系をDocker Composeで起動。accounts、revoked_tokensの2テーブル。named volumeで保持。ローカル接続は127.0.0.1:5432。

**Code Generation**: OpenAPI GeneratorのMaven pluginで `schema/auth/openapi.yaml` を検証し、Spring用APIインターフェースとHTTP DTOを生成する。生成コードは外側のweb adapter専用とし、domain/applicationへ依存させない。具体的な設定と未検証事項は[契約ガイド](contracts/README.md)を参照。

**Testing**: JUnit、MockMvc、Testcontainers PostgreSQL、ArchUnit、Vitest/Testing Library、Playwright。OpenAPI 3.1.1を正本とし、Schemathesis（pytest統合）で実応答の契約検証を行う。

**Target Platform**: PC上のJava/Node.jsとDocker Engine＋Compose。ブラウザは現行Chrome/Firefox。開発用Vite :5173から/apiをSpring Boot :8080へproxyする。外部公開時は同一オリジンHTTPS。

**Project Type**: 単一バックエンド＋React SPAのWebアプリ。アカウントは同一アプリ内の機能境界。

**Performance Goals**: specの登録〜ログイン2分、初心者5人中4人が補助なしで完了。同一ID10並列登録で成功1件。API応答時間の独自SLAは追加しない。

**Constraints**: JWT有効期間3600秒、期限の猶予0秒、自動更新なし。認証付き操作ごとに失効確認。CSRF保護。秘密情報をリポジトリ・応答・ログへ出さない。ドメイン/ユースケースにSpringやDBの型を持ち込まない。

**Scale/Scope**: 初版のアカウント機能、5エンドポイント、登録画面・ログイン画面・ログイン状態表示。管理画面・復旧・退会・全端末ログアウトは含めない。関連ライブラリの修正版は環境構築時に固定し、具体的バージョンをlockfileとWrapperへ記録する。

## Constitution Check

| 原則 | 設計前 | 設計後の確認 |
| --- | --- | --- |
| 仕様先行 | PASS: 承認済みspecが存在 | PASS: plan・契約・データ設計のみ作成 |
| 内向き依存 | PASS: 単一モジュール内で分離可能 | PASS: 内側のportを外側が実装、ArchUnitで検証 |
| 責務と簡潔さ | PASS: 2テーブルで対応可能 | PASS: 外部認証基盤・Redis・サービス分割なし |
| テスト可能性 | PASS: 受け入れ例あり | PASS: Clockと境界を差し替え、DB固有動作は結合テスト |
| 入力検証 | PASS: 文字数・形式が定義済み | PASS: 契約とドメインの両方に制約、境界値テストを指定 |

## Project Structure

### Documentation (this feature)

```text
specs/001-auth/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── README.md                     # ルートのschema/authを参照
└── checklists/requirements.md
```

API契約はルートの `schema/` に集約する。

```text
schema/
├── auth/openapi.yaml
└── game-recruitment/README.md       # 掲示板契約は次の設計段階で作成
```

`contracts/README.md` はSpec Kitが契約の場所を見つけるための案内として残す。
実装タスクは `tasks.md` に整理済み。

### Source Code (repository root)

以下の構成をimplement-authで実装した。

```text
backend/
├── pom.xml, mvnw, .mvn/
└── src/
    ├── main/java/com/gamerecruitment/
    │   ├── account/domain/
    │   ├── account/application/       # usecase, port, command/result
    │   ├── account/adapter/web/
    │   ├── account/adapter/security/
    │   ├── account/adapter/persistence/
    │   └── configuration/
    ├── main/resources/db/migration/
    └── test/java/com/gamerecruitment/ # unit, integration, architecture, contract
frontend/
├── package.json, package-lock.json
├── src/features/account/            # pages, API calls, validation
├── src/shared/api/                  # credentials, CSRF, error handling
└── tests/                           # components, browser journeys
tests/contract/                     # Schemathesis・認証fixture・Python依存lock
compose.yaml                        # PostgreSQLのみ
.env.example                        # ダミー値と必要な変数名のみ
```

**Structure Decision**: 1つのMavenモジュールと1つのReactアプリ。Springのcontroller/filter、暗号処理、SQLはadapterに置く。applicationはAccountRepository、PasswordHasher、TokenIssuer、RevokedTokenStoreとClockへ依存する。TokenIssuerはJWT型を公開せず、文字列・本人ID・識別番号・期限の単純な結果を返す。設定層が配線し、SQLトランザクションも外側で適用する。

## Design Flow

- 登録: CSRF → JSON/文字数検証 → ID正規化 → password hash → 一意制約付き保存 → 公開プロフィール。事前重複確認だけに依存せず、競合は409へ変換する。
- ログイン: CSRF → 認証情報確認 → JWT発行 → auth_token Cookie → CSRF Cookieクリア。存在しないIDもダミーハッシュで検証し、認証失敗の理由を統一する。
- 認証: CookieからJWT抽出 → 許可アルゴリズム/署名/issuer/audience/期限/必須claim → 失効確認 → 本人ID。存在しないアカウントは401。DBエラーは503。
- ログアウト: CSRF → JWT検証 → 有効なjtiを失効記録 → commit → Cookie削除 → 204。繰り返し実行は安全に成功させる。未ログインのログアウトを通せるよう、通常の認証拒否より前に専用処理を設計する。
- React: 初期表示でcsrf取得とme確認。更新要求はJSONとCSRF headerを送る。401なら本人表示を消してログインへ案内。403ではcsrf再取得後に再操作を促し、更新要求を自動再送しない。ログイン/ログアウト後もcsrfを取り直す。
- 同じブラウザで有効ログイン中の再ログインは409 ALREADY_AUTHENTICATEDとして、先にログアウトを促す。他ブラウザのログインには影響しない。

## Requirement Coverage

| 要件 | 設計先 | 検証 |
| --- | --- | --- |
| ACC-FR-001〜004・007 | POST /accounts、accounts | 有効・無効・Unicode境界・同時登録 |
| ACC-FR-005 | POST /auth/login | 正常・不一致・期限切れ後の再ログイン |
| ACC-FR-006・011 | POST /auth/logout、revoked_tokens | コピー再利用拒否・別端末維持・同一Cookieのタブ |
| ACC-FR-008 | accounts | 再起動・再ログインでも同一ID |
| ACC-FR-009 | GET /auth/me | 本人のみ、表示名重複でも別ID |
| ACC-FR-010 | JWT iat/exp、Clock | 3599秒可、3600秒不可、操作で延長なし |
| CSRF・失効障害 | GET /auth/csrf、security adapter | token欠落/不一致403、失効照会不能503 |

## Complexity Tracking

憲章違反なし。JWT＋失効一覧はセッション方式より手順が増えるが、JWT学習と即時ログアウトという合意した目的に必要な範囲とする。

## Validation Status

設計後の憲章チェックは通過。契約の確認状況は[契約ガイド](contracts/README.md)に記録する。
2026-10-10に実装と自動検証を実施。Java生成・verify、Reactの型検査・テスト・build、SchemathesisとPlaywrightを確認。手動の利用時間と初見利用者の評価は未実施。
実際のGitブランチはimplement-auth。plan-accountの設計は98a9c9aでコミット・プッシュ済み。Spec Kitの対象ディレクトリはspecs/001-authであり、番号とブランチ名は独立して管理する。


## Convergence修正の入力UI方針（2026-10-10）

T043の既存仕様への適合として、表示名とパスワードは改行を保持するtextareaを使用する。
パスワードはCSSの伏せ字表示を使用し、対応しないブラウザでは平文を表示せず入力を無効化して対応ブラウザへ案内する。
送信値は変形しない。ブラウザ結合テストで改行付き登録・ログインと伏せ字表示を確認する。
API契約・ドメインの入力条件は変更しない。
