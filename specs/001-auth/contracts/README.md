# アカウントAPI契約ガイド

正本: [openapi.yaml](../../../schema/auth/openapi.yaml)。業務要件: [spec.md](../spec.md)。
以下のAPIを実装済み。起動と検証の手順は[quickstart](../quickstart.md)を参照。

| メソッド | URL | 用途 | 成功 |
| --- | --- | --- | --- |
| GET | /api/auth/csrf | 更新操作の前にCSRF tokenを取得 | 200 |
| POST | /api/accounts | 登録（自動ログインしない） | 201 |
| POST | /api/auth/login | ID・パスワードでログイン | 200 |
| POST | /api/auth/logout | 今のログインだけ失効 | 204 |
| GET | /api/auth/me | 本人の公開プロフィールと期限 | 200 |

## CookieとCSRF

- auth_token: JWT。HttpOnly、Path=/api、SameSite=Lax、Domain未指定、Max-Age=3600。
- XSRF-TOKEN: CSRF token。JSから読み取り可、Path=/、SameSite=Lax、Domain未指定。セッションCookieとして扱う。
- 通常環境は両方Secure。HTTPで動かすloopback専用localプロファイルのみSecure=falseとする。
- Set-Cookieは複数行の独立したヘッダーで送る。削除はそれぞれ発行時と同じ名前・Path・DomainでMax-Age=0を指定する。
- GET csrfでCookieを実体化し、tokenにraw値、headerNameにX-XSRF-TOKENを返す。
- 全POSTでCSRF Cookieとheaderの照合が必要。JWT CookieがあってもCSRFを省略しない。
- Originが存在する場合は設定した同一オリジン（localはhttp://localhost:5173）と一致する必要がある。その他は403。
- 登録・ログイン・csrf取得では古い不正な認証Cookieで処理を遮断しない。meは認証必須。logoutは再試行可能な専用の認証処理を行う。
- 成功したログインとログアウトでCSRFを明示的にクリアする。独自のログイン処理でSpringの自動処理に依存せず、実際のCookie更新をテストする。
- 認証・CSRFを含む全応答はCache-Control: no-store。WebログにCookie・JWT・パスワードを記録しない。

## 画面側の操作順

1. Reactの初期表示でcsrfを取得し、meで本人を確認する。401は未ログインとして扱う。
2. 登録・ログインはJSONとCSRF headerを送る。fetchはsame-originのCookieを利用する。
3. ログイン成功後は本人表示を更新し、csrfを再取得する。JWTをJSで読み取らない。
4. ログアウト成功後は本人表示を消し、csrfを再取得する。別端末のログインは維持される。
5. 401は再ログインへ案内する。403ではcsrfを再取得して再操作を促すが、POSTを自動再送しない。
6. 503では失敗を表示し、ログアウト済みと表示しない。再試行成功で状態を確定する。

## 画面の範囲

- /register: ID・パスワード・表示名、項目エラー、成功後に/loginへ案内。
- /login: ID・パスワード、不一致の共通エラー、成功後に本人と期限を表示。
- ログイン状態表示: displayNameとログアウト操作。マイページの募集一覧は掲示板機能で実装する。
- API型をTypeScriptで宣言しても、外部データの実行時検証を省略しない。

## エラーと検証の順序

CSRF/Origin検証、必要な認証検証、JSON/業務入力検証、ユースケースの順を基本とする。
複数の条件に違反する場合は、先に判明したセキュリティエラーを返してよい。
Errorはcode・message・fieldsを必須とし、fieldsは入力値でなく項目名だけを含む。
IDの重複は409、認証情報不一致は401、JSON/入力違反は400、Content-Type不適合は415。
失効照会やログアウトcommitができないDB障害は503。想定外は500で内部詳細を隠す。

## 契約検証

Schemathesisを使用する（2026-10-10のユーザー指定）。実装時にPython依存の安定版を固定し、
`tests/contract/test_auth_contract.py`と認証fixtureを用意する。

- OpenAPIを読み込み、実際のAPIの応答コード、Content-Type、ヘッダー、本文を検証する。
- 正常系はCSRF取得→登録→ログイン→CSRF再取得を準備し、Cookieを維持する。CSRFヘッダーは最新のCookieと一致させる。
- 各ケースは独立した利用者・ログインで実行する。ログアウトで失効したJWTを次の正常系ケースへ流用しない。
- 未ログインやCSRF不一致の異常系と、正しい情報で成功する正常系を分ける。すべて401/403になる結果を契約テストの完了としない。
- OpenAPIのセキュリティ定義だけで、ログイン時のSet-CookieやCSRF更新を自動処理できるとは仮定しない。
- 期限境界、別端末維持、コピーしたJWTの拒否などは業務ルールの専用テストでも確認する。
- 生成リクエストは登録・ログアウトなどの状態変更を含むため、初期化可能なテスト専用DBとプロファイルに向ける。

### 確認状況

- 過去の配置でOpenAPIの形式チェックを1回実施。その後に配置・入力patternの変更あり。
- 現在の配置、文書の参照、タスクの形式は確認済み。
- Schemathesisの読込・実APIの応答検証を実施。全5操作の正常系・異常系・生成入力を確認。

公式資料: [CLI](https://schemathesis.readthedocs.io/en/stable/reference/cli/)、
[認証](https://schemathesis.readthedocs.io/en/stable/guides/auth/)、
[Python API](https://schemathesis.readthedocs.io/en/stable/reference/python/)。


## OpenAPIからのJavaコード生成（2026-10-10追加）

ユーザー指定によりOpenAPI Generatorを採用する。OpenAPIを正本として、Mavenの
`generate-sources`段階で検証・生成を行う。`skipValidateSpec`はfalseを明示し、
検証・生成のエラーでビルドを停止する。別途CLIの`validate`でも仕様ファイルを確認できる。

- generatorは`spring`、libraryは`spring-boot`、`interfaceOnly=true`、`skipDefaultInterface=true`。
- Spring Boot 4対応版を固定し、`useSpringBoot4=true`とJacksonの対応設定を確認する。
- API interfaceとリクエスト・レスポンスDTOを `backend/target/generated-sources/openapi/` に生成する。生成コードを手で編集せず、毎回OpenAPIから再生成する。
- 生成先packageは `com.gamerecruitment.account.adapter.web.generated.api` と `.model`。手書きControllerが生成interfaceを実装し、DTOとapplicationの型を変換する。
- domain/applicationは生成DTOに依存しない。JWT、CSRF、DB操作、業務ルールは自動生成されるものとして扱わない。
- 生成成功とJavaコンパイル成功を別々に確認する。OpenAPI 3.1.1の`const`、`additionalProperties: false`、Unicodeコードポイントの文字数が生成後にも意図どおり扱われるか、既存の契約・境界値テストで確認する。生成annotationだけに検証を任せない。
- 生成したメソッドの型に合わせた実装はコンパイラで確認できるが、実際の応答内容・Cookie・業務動作はSchemathesisと振る舞いテストで確認する。

OpenAPI Generatorによる仕様検証、MavenのJavaコード生成・コンパイル・verifyを実施済み。固定バージョンと実行コマンドはquickstartに記録。

公式資料: [Maven plugin](https://openapi-generator.tech/docs/plugins/)、
[Spring generator](https://openapi-generator.tech/docs/generators/spring/)、
[仕様のvalidateコマンド](https://openapi-generator.tech/docs/usage/#validate)。


### 現在のauth仕様の検証結果（2026-10-10）

- 対象: `schema/auth/openapi.yaml`（OpenAPI 3.1.1）
- ツール: OpenAPI Generator CLI 7.26.0、Dockerで実行。
- イメージdigest: `sha256:a304ddf1e2e5f24f68fa3153568d6174cea4959d09aa8e3db6d526fd0782326d`
- 終了コード: 0。結果: `No validation issues detected.`
- 追加確認: 全component JSON Schemaの形式と、13件のexampleをJSON Schema Draft 2020-12で検証（UUID/date-timeのformat検証を含む）。すべて成功。
- 検証したファイルのSHA-256: `6f11294db3baf2845fd04e10dcab6dccfbd010e0d07c66827f902737de24b97d`

再実行コマンド（リポジトリルートから）:

```bash
docker run --rm --network none \
  --mount type=bind,src="$PWD/schema/auth",dst=/schema,readonly \
  openapitools/openapi-generator-cli:v7.26.0 \
  validate -i /schema/openapi.yaml
```

仕様ファイルの形式・構造の検証結果であり、Javaコード生成・コンパイルや実APIの応答・業務動作の合格を意味しない。


### 未定義のHTTPメソッド

定義されたURLに未対応のHTTPメソッドでアクセスした場合、405、Allowヘッダー、
METHOD_NOT_ALLOWEDの共通Errorを返し、状態を変更しない。HTTPのHEAD/OPTIONSは
フレームワークによる処理を許容する。Schemathesisの未対応メソッド生成で確認する。

Cookieなし・不正・期限切れのログアウトでは、失効を記録する対象がないためDBトランザクションを開始しない。有効なJWTだけを失効処理のトランザクションへ渡す。


### 実装後の検証結果（2026-10-10）

- OpenAPI Generator 7.26.0: METHOD_NOT_ALLOWED追加後も`No validation issues detected.`、終了コード0。
- 最終契約のSHA-256: `eb90ba7324392f24efce44ca7799cc5275f50be23c75f06217d4210a28d11e2c`
- Maven verify: JUnit・MockMvc・Testcontainers PostgreSQL・ArchUnitの22テスト成功、skipなし。
- React: typecheck/build成功、Vitestの14テスト成功。
- Schemathesis 4.30.1: シナリオ2件＋生成入力を含む5操作、計7テスト成功。
- Playwright/Chromium: 登録から複数タブ・別端末・JWTコピーの拒否・再ログインと重複ID表示、2テスト成功。
- 初期化SQL: 専用DBへの2回の実行成功。開発DBに対する実行は行わない。
- 手動の利用時間計測と初見5人の評価は未実施。自動テスト合格をその達成と見なさない。

生成テストで見つかった未対応HTTPメソッドの500を405へ修正し、APIで不要なmultipart解析を無効化した。
ブラウザテストで見つかった複数タブのCSRF更新競合は、POST直前に最新Cookieのraw値を使うことで対応した。
Cookie保持を前提とする認証プローブの自動再送は契約照合に含めず、認証とCSRFの拒否を明示的なテストで検証する。

最終版jarでAPIを再起動し、失効済みJWTは401、有効JWTは200になることを確認。専用DBの初期化後に同じIDでの登録を2回確認した。
