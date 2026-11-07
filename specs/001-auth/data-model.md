# アカウントのデータ設計

対象: [spec.md](spec.md)。登録情報と失効情報を管理し、募集のデータは掲示板設計に委ねる。

## accounts

| 項目 | 保存形式 | 制約・意味 |
| --- | --- | --- |
| id | UUID | 主キー。表示名やログインIDと独立した本人ID |
| login_id | varchar(30) | NOT NULL、UNIQUE、ASCII小文字英数字と_、3〜30文字 |
| display_name | varchar(30) | NOT NULL、1〜30コードポイント、Unicode空白のみ不可、重複可 |
| password_hash | text | NOT NULL。Argon2idのsalt・パラメーター込みハッシュ |
| created_at | timestamptz | NOT NULL、作成日時 |

- 入力IDのASCII英字を小文字化して保存・検索する。前後の空白を勝手に削除しない。
- DBはUTF-8。IDの形式/長さ、表示名の長さをCHECK制約でも保証する。空白判定とパスワード長はドメインで検証する。
- パスワードは15〜128コードポイント。平文は保存せず、ログとレスポンスにも出さない。
- 登録は1トランザクション。UNIQUE競合を409 LOGIN_ID_TAKENへ変換する。
- 公開プロフィールはid、displayNameのみ。非公開login_id、password_hashを含めない。
- 募集者・参加者はaccounts.idを参照し、表示名を本人識別に使用しない。

## revoked_tokens

| 項目 | 保存形式 | 制約・意味 |
| --- | --- | --- |
| jti | UUID | 主キー。無効化したサーバー発行JWTの識別番号 |
| expires_at | timestamptz | NOT NULL、JWTの期限 |
| revoked_at | timestamptz | NOT NULL、無効化時刻 |

- JWT本体は保存しない。ログイン時には行を作らず、失効時だけ保存する。
- 同じjtiの登録は冪等。主キー競合をログアウトの失敗にしない。
- expires_atに削除用インデックスを設定し、1時間ごとにexpires_at <= 現在時刻の行を削除する。
- 認証はJWT自身の期限も検証するので、削除ジョブ停止で期限切れJWTが有効になることはない。
- 失効のcommit完了前にログアウト成功を返さない。DB障害では503とし、ログアウト完了と表示しない。
- テスト環境の全体初期化では、関連する募集・参加を含めて整合する順序で削除する。一般利用者向け削除APIは設けない。

## JWT claims（保存テーブルではない）

| claim | 意味 |
| --- | --- |
| iss | 固定値 game-recruitment-board |
| aud | 固定値 game-recruitment-api |
| sub | accounts.idのUUID文字列 |
| jti | ログインごとに生成するUUID |
| iat | 発行時刻、UTCのepoch秒 |
| exp | iat + 3600、UTCのepoch秒 |

HS256を固定して検証。必須claimの欠落・型違い・未知のissuer/audience・改ざん・未来のiatを拒否する。
nbfが存在すれば現在時刻以降の利用制限も検証する。exp <= 現在時刻で拒否し、既定のclock skewにより有効期間を延長しない。
サーバーのClockを時刻判定の正本とし、表示用expiresAtは日本時間に変換できる日時として返す。

## 状態遷移

```text
未ログイン → 認証成功 → 有効（1時間）
有効 → ログアウトcommit → 失効
有効 → exp到達 → 期限切れ
失効/期限切れ → 再ログイン → 新しいjtiで有効
```

各ログインは独立する。同じCookieを共有するタブは同一ログインを共有する。
失効・期限切れのJWTが再び有効になることはない。有効期間を操作で延長しない。

## データを持たない判断

- CSRF Cookieは認証情報ではない。JWTと別の名前で扱い、サーバー側のログインテーブルを追加しない。
- 権限/ロール、メール、退会フラグ、refresh tokenは初版では保存しない。
- ログイン情報の永続化はaccounts、ログアウト情報の永続化はrevoked_tokensで確認する。
