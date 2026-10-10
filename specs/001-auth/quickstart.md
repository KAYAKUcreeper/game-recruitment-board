# アカウント機能の起動・検証ガイド

## 実装範囲

登録、ログイン、本人・固定期限の確認、ブラウザ単位のログアウトを実装した。
募集・参加・募集一覧は別機能で、未実装。

## 固定バージョンと前提

Linux/WSL x86_64で確認。Node.js 22.12以上の22系、Python 3.12以上、Dockerを実行できるユーザーが必要。

| 対象 | 確認した版 |
| --- | --- |
| Temurin JDK | 25.0.4.1+1、配布元SHA-256照合済み |
| Maven / Wrapper | 3.10.0 / 3.3.4 only-script |
| Spring Boot / OpenAPI Generator | 4.1.1 / 7.26.0 |
| React / React DOM | 19.3.0 / 19.3.0 |
| TypeScript / Vite | 5.9.3 / 8.3.4 |
| Node.js / npm | 22.12.0 / 11.6.0（既存10.9.0の依存解決内部エラーを回避） |
| PostgreSQL | 18.4、compose.yamlでイメージdigestも固定 |
| Docker Compose | 5.5.1 |
| Vitest / Playwright | 4.1.11 / 1.64.0 |
| Schemathesis / pytest | 4.30.1 / 9.1.1、uv.lockで依存固定 |

JavaとReactはホスト上、PostgreSQLだけDockerで実行する。
SQLはSpring JDBC、スキーマ変更はFlyway。Testcontainersは独立したDBを起動する。

## 初回準備

リポジトリルートから実行する。

```bash
./scripts/install-local-jdk.sh
./scripts/init-local-env.sh
docker compose up -d --wait db
```

JDKはGitの対象外の`.tools/`へ取得する。すでにJDK 25を使える場合は導入を省略できる。
`.env`にはランダムな開発用DBパスワードと32バイトのJWT署名鍵を作る。既存の.envは上書きしない。
DBポートは127.0.0.1:5432。volumeはPostgreSQL 18に合わせて/var/lib/postgresqlへマウントする。

## アプリ起動

```bash
./scripts/run-backend.sh
```

localプロファイルは127.0.0.1:8080で起動する。起動時にFlywayが2つのテーブルを用意する。
通常はSecure Cookieを使い、local/contractのloopback HTTPだけ例外とする。
Non-Secure Cookie設定で公開アドレスに起動する構成は起動時に拒否する。

別ターミナルで:

```bash
cd frontend
npm ci
npm run dev
```

http://localhost:5173/register を開く。Viteが/apiをSpring Bootへ転送する。
ブラウザからDBへ直接接続しない。npm ciはlockfileどおりに依存を用意する。

## Javaコード生成と自動検証

このセッションのローカルJDKを利用する場合、リポジトリルートで:

```bash
export JAVA_HOME="$PWD/.tools/jdk-25.0.4.1+1"
export PATH="$JAVA_HOME/bin:$PATH"
export MAVEN_USER_HOME="$PWD/.tools/maven-user"
./backend/mvnw -f backend/pom.xml -Dmaven.repo.local="$PWD/.tools/m2" verify
```

generate-sourcesでOpenAPIを検証し、API interfaceとDTOをbackend/target/generated-sources/openapiへ生成する。
生成ファイルを手で編集しない。Controllerが生成interfaceを実装し、DTOはweb adapter内で利用する。
文字数の生成@Sizeに依存せず、ドメインでUnicodeコードポイントを数える。
const・未知JSON項目・Cookie・CSRFは専用テストでも確認する。
verifyはJUnit、MockMvc、PostgreSQL結合、ArchUnitを実行する。Dockerへのアクセスが必要。

画面側は:

```bash
npm --prefix frontend run typecheck
npm --prefix frontend run test -- --run
npm --prefix frontend run build
```

## ブラウザ・Schemathesis用の独立環境

開発DBを消さないよう、game_recruitment_contractを使う。

```bash
./scripts/create-contract-db.sh
./scripts/run-backend.sh contract
```

APIが127.0.0.1:8081で起動したことを確認する。許可Originはhttp://localhost:5174。
初回起動でテーブルができた後、別ターミナルでテストデータを初期化できる。

```bash
./scripts/reset-contract-db.sh
```

SQLでもDB名を確認し、専用DB以外は拒否する。通常利用者向けの削除APIは設けない。
掲示板実装後は関連する募集・参加を含めた初期化へ拡張し、固定ゲーム一覧は保持する。

### ブラウザテスト

```bash
cd frontend
npx playwright install --with-deps chromium
npm run test:e2e
```

Playwrightが5174のViteを起動する。別途立ち上げた8081のAPIへ接続する。
Linuxの初回ブラウザ導入では、実行ライブラリのインストールに管理者権限が必要な場合がある。
このセッションではシステムを変更せず、不足したlibnspr4/libnss3/libasound2t64を.tools/browser-libsへ配置した。
同じ配置を使う場合はリポジトリルートから:

```bash
PLAYWRIGHT_BROWSERS_PATH="$PWD/.tools/playwright" \
LD_LIBRARY_PATH="$PWD/.tools/browser-libs/usr/lib/x86_64-linux-gnu" \
npm --prefix frontend run test:e2e
```

### Schemathesisの契約テスト

```bash
uv sync --project tests/contract --locked
uv run --project tests/contract pytest tests/contract/test_auth_contract.py
```

全5操作の正常系、明示的な400/401/403/409、生成入力を検証する。
生成テストはseed=20261010、max_examples=20。各入力で新しいSession/利用者を作り、Cookie・CSRFを準備する。
ログイン・ログアウト後はCSRFを取得し直す。認証失敗だけで合格としない。
応答コード、Content-Type、ヘッダー、本文と500の有無を検証する。
Cookie保持時に認証プローブがCookieを再送する影響を避け、認証・CSRFの失敗は明示的なケースで検証する。
生成テストのOPTIONS *など、API操作以外のサーバー応答にはAPI用no-storeの追加assertを適用しない。
Cookie・パスワードを含む失敗時の再現コマンドや生ログは共有しない。

## 検証結果（2026-10-10）

OpenAPIのGenerator検証、Javaコード生成・コンパイル、画面のtypecheck/buildを確認。
JUnit・結合テスト、画面テスト、Schemathesis、ブラウザの結果は契約ガイドの確認状況に記録する。
専用DBの初期化は2回成功。初期化後に同じIDを再利用する登録を2回確認済み。最終版jarの再起動後も、失効済みJWTの401と有効JWTの200を確認。

- ACC-SC-002: 10並列登録で成功1件を自動テストで確認。
- ACC-SC-003: 実装した正常系・異常系・入力境界の自動検証を実施。
- ACC-SC-001（利用者の登録〜ログイン2分）: 手動計測は未実施。自動ブラウザ操作の時間とは区別する。
- ACC-SC-004（初見5人中4人）: 手動評価は未実施。実装完了をこの目標の達成として扱わない。

## 停止・データ保持

JavaとViteは各ターミナルでCtrl+C。

```bash
docker compose stop db
```

stopは停止のみ。docker compose downでもnamed volumeは残る。
down -vはデータを削除するため、通常の停止手順には使わない。
