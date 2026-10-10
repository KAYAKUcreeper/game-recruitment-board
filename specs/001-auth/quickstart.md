# アカウント機能の起動・検証ガイド

## 現時点の状態

これは実装後の検証手順であり、backend/、frontend/、compose.yamlはまだ未作成。
API応答の契約検証にはSchemathesisを使う。API本体の実装後に実行する。動作テストに合格したという記録ではない。

## 前提

- Temurin JDK 25、Node.js 22.12以上の22系、npm、Docker EngineとCompose v2。
- Java/MavenはPC上、React/ViteもPC上で実行。PostgreSQLだけコンテナ内で動かす。
- 実装でMaven Wrapper 3.10.0、npm lockfile、postgres:18系の固定イメージ、.env.exampleを用意する。
- 開発専用DB名game_recruitment、サービス名db、volume名postgres_dataとする。DBポートは127.0.0.1:5432へ限定する。
- PostgreSQL 18の公式イメージに合わせ、volumeは/var/lib/postgresqlへマウントする。テストはTestcontainersの独立DBを使い、開発DBを消さない。
- JWTの署名鍵、DBパスワードはローカルの無視対象設定へ保存する。環境変数はAPP_JWT_SECRET_BASE64、DB_URL、DB_USERNAME、DB_PASSWORD。秘密値を文書へ記載しない。
- 実装でlocalプロファイルをloopback専用にし、CookieのSecure例外を公開環境へ持ち込まない。

## 実装後の起動

リポジトリルートでDBを起動する。

```bash
docker compose up -d db
docker compose ps
```

upはコンテナを起動し、-dはバックグラウンド実行。初回はPostgreSQLのイメージを取得する。
healthcheckでDBが利用可能になってから、別ターミナルでJava側を起動する。

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

mvnwはプロジェクトで指定したMavenを使用する入口。初回は依存ライブラリを取得する。
必要な環境変数をこのプロセスへ渡す。起動時にFlywayが未適用のDB変更を適用する。

さらに別ターミナルで画面を起動する。

```bash
cd frontend
npm ci
npm run dev
```

ciはlockfileどおりに依存を入れ、devはViteを起動する。http://localhost:5173へアクセスする。
ブラウザは/apiを同じホストへ送り、Viteがlocalhost:8080へ転送する。
ブラウザからDBへ直接接続しない。

## 自動検証（実装時に対応するスクリプトを用意する）

```bash
cd backend
./mvnw verify
```

ユニット・DB結合・MockMvc・依存方向・契約の検証を含める。DockerがTestcontainersから利用可能であること。

```bash
cd frontend
npm ci
npm run typecheck
npm run test -- --run
npm run build
npm run test:e2e
```

typecheckはstrictの型検査、testはコンポーネント、test:e2eは起動済みアプリへのPlaywright検証。
実装時に必要なブラウザの導入手順と起動設定を追加する。認証期限はテスト用Clockで進め、1時間待つテストにしない。

## 受け入れシナリオ

1. /registerで新しいIDを登録する。成功201、アカウントid/displayNameのみ返り、JWTは発行されない。
2. 同じIDと大小違いのIDは409。10並列の同一ID登録は成功1件。
3. /loginで正しい情報を送る。200、HttpOnly auth_token Cookie、1時間後のexpiresAt。誤ったID/パスワードは同じ401形式。
4. ログイン直後にcsrfを再取得し、meで本人表示を確認する。Cookie値をJSから取得できないことを確認する。
5. CSRF headerを欠くPOSTは403。正しいtokenでも別Originは403。失敗時に登録や失効などの変更がないこと。
6. IDの2/3/30/31文字、表示名の0/1/30/31文字、パスワードの14/15/128/129文字、Unicode・空白のみを検証する。
7. 異なるブラウザコンテキストA/Bで同じアカウントへログイン。Aでログアウト後、AのJWTのコピーを再送してもmeは401。Bは200を維持。
8. A内の2つのタブはログアウト後、次のmeで両方401。即時プッシュ通知は不要。
9. ログインから3599秒では200、3600秒ちょうどで401。途中の操作で期限は変わらない。
10. ログアウトの再送、期限切れ、Cookieなしは、正しいCSRFを付ければ204。失効DB障害は503となり、成功表示を出さない。
11. 再起動してもaccounts・失効記録が残る。期限後の失効記録整理でもJWTが再び有効にならない。
12. 登録〜ログイン2分以内、初見5人中4人が補助なしで完了するか計測する。未実測の数値を達成済みにしない。

## 停止とデータ保持

ルートで `docker compose stop db` は停止だけを行う。
`docker compose down` はコンテナとネットワークを削除するがnamed volumeは残す。
`docker compose down -v` はvolumeも削除しデータが消えるため、通常の停止手順には使わない。
テスト用全体初期化はテスト環境に限定した手段として実装し、固定ゲーム一覧は保持する。

## Schemathesisによる契約検証（API実装後）

T040で`tests/contract/`にPython依存と認証fixtureを実装し、Schemathesisの版をlockする。
実装後は、テスト専用DBへ接続したAPIを起動してから、リポジトリルートで実行する。

```bash
uv run --project tests/contract pytest tests/contract/test_auth_contract.py
```

fixtureは起動URLの設定、独立したアカウント、CSRF Cookie/header、ログインCookieの準備を行う。
POSTのたびに使えるCSRFを渡し、認証状態が変わる操作後には再取得する。
正常系がすべて401/403になる場合は認証設定の不足として直し、合格扱いにしない。

CLIで単一の公開操作を確認する例（導入後、テスト用APIのポート8080の場合）:

```bash
uv run --project tests/contract schemathesis run schema/auth/openapi.yaml --url http://localhost:8080/api --include-operation-id getCsrfToken --checks status_code_conformance,content_type_conformance,response_schema_conformance
```

この例はcsrf取得だけを確認する。5操作の認証付き契約テストは上記のpytest fixtureで行う。
`--url`には/apiまで含める。APIをlocalhost:5173経由で検証する場合はURLをそちらへ合わせる。
Originヘッダーを送る場合は許可されたOriginと一致させる。
依存導入時に使用版のCLIと上記オプションの対応を確認する。

OpenAPIファイルだけを読み込むには、同じ依存環境でPythonの
`schemathesis.openapi.from_path("schema/auth/openapi.yaml")`を使用できる。
読込成功は、実装の応答や業務ルールが正しいことを示すものではない。

契約テストの生成・seed・実行結果は実装後に記録する。Cookieとパスワードを含む生の通信ログは共有しない。
現在はAPI未実装のためSchemathesisによる実API検証は未実施。

公式手順: [CLI](https://schemathesis.readthedocs.io/en/stable/reference/cli/)、
[認証](https://schemathesis.readthedocs.io/en/stable/guides/auth/)。


## OpenAPIの検証・Javaコード生成（環境構築後）

T002でMaven pluginを設定した後、リポジトリルートから実行する。

```bash
./backend/mvnw -f backend/pom.xml clean generate-sources
./backend/mvnw -f backend/pom.xml compile
```

最初のコマンドでOpenAPI検証とAPI interface・DTO生成、次でJavaとしての整合性を確認する。
生成物は`backend/target/generated-sources/openapi/`に置く。
現在はbackend/Wrapper/Javaが未導入のため、このコマンドはまだ実行できず、検証結果も未取得。
API起動後には上記とは別にSchemathesisで実際の応答を検証する。


### API未実装でも実行できる仕様ファイル検証

2026-10-10にDocker版OpenAPI Generator 7.26.0の`validate`を実行し、
`No validation issues detected.`（終了コード0）を確認した。
再実行コマンドと検証範囲は[契約ガイド](contracts/README.md#現在のauth仕様の検証結果2026-10-10)に記録。
Javaコード生成・コンパイル・Schemathesisによる実API検証は未実施。
