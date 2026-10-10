# アカウント機能の設計判断

調査日: 2026-10-03。対象は[アカウント仕様](spec.md)。ユーザー合意は[共通設計方針](../002-game-recruitment/api-design-decision.md)を参照。

## 1. 構成とバージョン

- Decision: Java 25、Spring Boot 4.1.1、Maven 3.10.0（Wrapper）、React 19.3、TypeScript 5.9、Vite 8、Node.js 22.12以上の22系、PostgreSQL 18系を採用する。JDK配布はEclipse Temurin 25。プレビュー機能は使わない。
- Rationale: Java/Spring Bootの対応範囲を確認し、アプリとDBのプロセスを分離した通常のローカル開発にする。既存Node.js 22.12.0はViteの最低要件を満たす。実装時に各選定系列の修正版を確認し、JDK更新番号、npm lockfile、Maven Wrapper、コンテナタグとdigestで実際の版を固定する。Reactとreact-domは同版にする。
- Alternatives considered: Gradle、サーバー描画、全アプリのDocker化はユーザー選択と異なるため採用しない。Springの関連依存はBootの依存管理に従い、個別に最新へ上書きしない。
- Sources: [Boot](https://docs.spring.io/spring-boot/system-requirements.html)、[Maven](https://maven.apache.org/download.cgi)、[React](https://react.dev/versions)、[TypeScript](https://www.typescriptlang.org/docs/handbook/release-notes/typescript-5-9.html)、[Vite](https://vite.dev/guide/)、[PostgreSQL](https://www.postgresql.org/support/versioning/)、[Temurin](https://adoptium.net/supported-platforms/)。

## 2. Clean Architectureと保存

- Decision: 単一Spring Bootアプリ、単一Mavenモジュール。account/domain、account/application、account/adapter、configurationで依存を分離する。SQLはJdbcClientを用いた外側のアダプターで実行し、Flywayでスキーマ変更を管理する。
- Rationale: 小さい2テーブルでSQLと制約を学びながら、ドメインをSpring/JDBC/JWTから独立させる。Repository、PasswordHasher、TokenIssuer、RevokedTokenStoreの境界とClockを内側に置く。トランザクション境界は外側のデコレーターで適用する。
- Alternatives considered: JPAは今回の単純な保存に必要な機能以上の学習項目が増える。サービス分割、汎用リポジトリ、認証専用サービスは追加しない。

## 3. 認証とログアウト

- Decision: Spring SecurityのJWTエンコード・デコード機能を使用する。Cookie専用のBearerTokenResolverを外側のアダプターで実装する。署名はHS256固定、暗号学的乱数32バイト以上の鍵を環境設定から読み込む。単一発行者・単一API向けのiss/aud、UUIDのsub/jti、iat/expを検証する。exp = iat + 3600秒、期限判定の許容遅延は0秒とする。JWTにログインID、表示名、パスワードを載せない。
- Rationale: JWT学習を実現しつつ暗号処理を自作しない。現在時刻がexp以上なら拒否し、署名だけでログインを判定しない。
- Decision: JWTはauth_token Cookie（HttpOnly、SameSite=Lax、Path=/api、Domainなし、Max-Age=3600）へ保存する。通常環境はSecure必須。ローカル開発のloopback HTTPだけ専用localプロファイルでSecure=falseを許容する。Bearerヘッダー・URL・localStorageによるJWT受付/保存は行わない。
- Decision: ログアウトでは検証済みjtiをPostgreSQLへ記録し、成功コミット後に同じ属性のCookieをMax-Age=0で削除する。期限前の有効JWTを毎回失効照会する。DB障害時は認証を通さず503とする。ログアウト済み・期限切れ・Cookieなしの再ログアウトは204でCookieを消す（CSRF検証は必要）。署名を検証できないJWTを失効一覧へ登録しない。
- Rationale: Cookie削除だけではコピーされたJWTが使えるため、サーバー側で再利用を拒否する。失効確認を通過して既に開始した処理を後から取り消すことまでは要求しない。ログアウト応答後に開始した認証は拒否する。
- Alternatives considered: セッション方式は簡単だがJWT学習の希望に合わない。リフレッシュトークン・全端末ログアウトは対象外。Redisは追加せず、失効一覧は期限後に定期削除（1時間ごと）する。
- Sources: [JWT](https://www.rfc-editor.org/rfc/rfc7519)、[Spring JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)。

## 4. CSRFとReactからの通信

- Decision: 開発時はViteが/apiをlocalhost:8080へ転送し、ブラウザはlocalhost:5173の相対URLだけを使用する。公開時も同一オリジン構成を前提とし、任意オリジンへの認証付きCORSは許可しない。
- Decision: Spring SecurityのCookieCsrfTokenRepositoryとSPA対応のリクエストハンドラーを使用する。XSRF-TOKENはJSから読み取り可、Path=/、Domainなし、SameSite=Lax、SecureはJWT Cookieと同じ環境規則。GET /api/auth/csrfでトークンを実体化し、CookieとJSONで返す。JSONはCookieと同じraw値とheaderNameを含める。更新操作はX-XSRF-TOKENヘッダーを必須にし、Cookieとの照合を行う。
- Decision: 登録・ログイン・ログアウトもCSRF保護する。ログイン成功・ログアウト完了時にCSRF Cookieをクリアし、Reactは再度csrfを取得する。SPAの送信値とBREACH対策のマスク値の扱いが一致することを結合テストする。Originがあれば許可したオリジンと一致させ、不一致は403。OriginなしはCSRFトークン検証を省略しない。
- Rationale: HttpOnlyはJWTの読み取りを防ぐが、Cookieの自動送信によるCSRFを防がない。サブドメインでCookieを共有せず、トークン照合とOrigin確認を組み合わせる。
- Alternatives considered: localStorage保存は採用しない。独自のCSRF暗号処理ではなくフレームワークの対応を使う。
- Source: [Spring CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)。

## 5. パスワードと入力

- Decision: Argon2idをSpring Security PasswordEncoderのアダプターで使用する。初期パラメーターはmemory=19456KiB、iterations=2、parallelism=1、salt=16bytes、hash=32bytesとし、Bouncy Castleを使用する。保存値はアルゴリズムとパラメーターを含むハッシュのみ。
- Rationale: 合意した15〜128コードポイント・日本語を扱い、bcryptの72バイト制限による切り捨てを避ける。パスワードをtrim・大小変換・Unicode正規化しない。不正なUnicodeのJSONは400とし、パスワードをエンコードする際に文字を置換・切り捨てない。
- Decision: IDはASCII小文字へ正規化しDBの一意制約で競合を防ぐ。空白のみはUnicode White_Spaceの集合で定義する。文字数はJava codePointCount、Reactはコードポイント単位で確認する。UTF-16長をそのまま制約として使用しない。
- Alternatives considered: 平文・可逆暗号は不採用。形式チェックだけで入力を信頼せず、内側でも不変条件を検証する。
- Source: [OWASP password storage](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)。

## 6. 契約とテスト

- Decision: OpenAPI 3.1.1を正本にする。ユーザーの追加指定によりOpenAPI GeneratorでSpring API interface・HTTP DTOを生成し、手書きのコントローラーが実装する。生成DTOはweb adapter内で使用する。仕様検証・生成をMavenのgenerate-sourcesで行い、業務ロジックは手書きで実装する。ReactのAPI型は契約から生成してもよいが、実行時検証の代替にしない。
- Decision: JavaのユニットテストはBoot管理のJUnitと差し替えClock、境界はMockMvc、DBはTestcontainers PostgreSQL、依存方向はArchUnit。ReactはVitestとTesting Library、ブラウザ結合はPlaywrightを使用する。
- Decision: ユーザー指定により、API契約テストはSchemathesisに統一する。OpenAPIファイルを読み込み、稼働中のテスト用APIの応答コード・Content-Type・ヘッダー・本文と契約の一致を検証する。Cookie・期限・失効・同時登録の業務ルールは独立した振る舞いテストでも確認する。
- Decision: Schemathesisのpytest統合と認証fixtureを用意し、CSRF取得→登録→ログイン→CSRF再取得の準備と、logoutによる失効後の再準備を明示する。拒否応答だけで検証完了にしない。導入時に対応する安定版を確認し、tests/contract/の依存lockへ固定する。
- Rationale: 文書の妥当性と実装が契約を守ることを別々に検証する。ローカル実測前に性能達成を断定しない。
- Sources: [OpenAPI 3.1.1](https://spec.openapis.org/oas/v3.1.1.html)、[Schemathesis CLI](https://schemathesis.readthedocs.io/en/stable/reference/cli/)、[認証設定](https://schemathesis.readthedocs.io/en/stable/guides/auth/)、[Python API](https://schemathesis.readthedocs.io/en/stable/reference/python/)。

## 調査結果

設計を止める未確定事項はない。補助ツールの修正版固定と実際のビルド互換性は、実装の環境構築タスクで検証する。
設計時はJava未導入だったが、2026-10-10にTemurin 25を専用領域へ配置し、ビルド・実行・テストを確認した。実装結果はquickstartと契約ガイドを参照。
