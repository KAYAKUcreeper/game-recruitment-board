# 掲示板の起動・受け入れ検証計画

これは設計時のガイド。掲示板コード・マイグレーション・テストは未実装であり、以下は実装後の確認手順である。
既存の環境/秘密設定/認証/停止手順は[auth quickstart](../001-auth/quickstart.md)を使う。

## 環境と契約

Java 25、Node.js 22系、既存npm lock、PostgreSQL 18.4、Docker Compose、Python契約環境を再利用。
契約: [OpenAPI](../../schema/game-recruitment/openapi.yaml)、保存/状態: [data-model](data-model.md)、HTTP/エラー: [契約ガイド](contracts/README.md)。

リポジトリルートから、実装後に:

```bash
./scripts/init-local-env.sh
docker compose up -d --wait db
./scripts/run-backend.sh
```

別ターミナル:

```bash
npm --prefix frontend ci
npm --prefix frontend run dev
```

http://localhost:5173/ を開く。募集者と参加者は別ブラウザcontextで登録・ログインする。V3/V4はFlywayが実装後の起動時に適用する。

## 自動検証（実装後）

```bash
export JAVA_HOME="$PWD/.tools/jdk-25.0.4.1+1"
export PATH="$JAVA_HOME/bin:$PATH"
export MAVEN_USER_HOME="$PWD/.tools/maven-user"
./backend/mvnw -f backend/pom.xml -Dmaven.repo.local="$PWD/.tools/m2" verify
npm --prefix frontend run typecheck
npm --prefix frontend run test -- --run
npm --prefix frontend run build
```

Java verifyは2契約の検証・DTO生成、domain/application、MockMvc、実DB競合、ArchUnit、auth回帰を含める。frontendは応答の実行時検証、エラー/空/期限・権限表示を含める。

専用DBのAPIで契約/ブラウザを検証する:

```bash
./scripts/create-contract-db.sh
./scripts/run-backend.sh contract
```

別ターミナルで、掲示板表を含むよう拡張済みの初期化SQLを使って:

```bash
./scripts/reset-contract-db.sh
uv sync --project tests/contract --locked
uv run --project tests/contract pytest tests/contract/test_auth_contract.py tests/contract/test_recruitment_contract.py
```

test_recruitment_contract.pyは実装時に作成。CSRF取得、複数の本人fixture、未来の募集、参加者/未参加者/募集者、全12操作の正常/異常、schema照合を含める。更新fixtureは専用DBへ向け、成功がない401/403だけの合格にしない。

ブラウザ（既存ローカル配置を使う場合）:

```bash
PLAYWRIGHT_BROWSERS_PATH="$PWD/.tools/playwright" \
LD_LIBRARY_PATH="$PWD/.tools/browser-libs/usr/lib/x86_64-linux-gnu" \
npm --prefix frontend run test:e2e
```

通常環境のブラウザ導入はauthのガイドに従う。Chrome/Firefoxの対象と実行したbrowserを結果に明示する。

## 受け入れシナリオ

1. **公開検索（US1）**: 未ログイン・不正Cookieでも一覧/詳細が開く。3ゲームの絞り込み、受付のみ、開始時刻/作成時刻/UUID順、21件以上の次ページ、空表示を確認。公開JSONにmeetingInfo/loginId/password/JWTを含めない。
2. **作成（US2）**: 募集者を除く1/9人、タイトル1/100、本文1/2000、改行/Unicode、JST分単位の未来時刻で作成。直後0人、マイページ掲載。0/10/小数/文字列・空白・過去/現在/不実在日時・未知ゲーム/JSON項目は拒否。本文編集・募集削除は不可。
3. **参加（US3）**: 別本人が承認なし参加し人数/参加者/マイページが変わる。本人の募集・重複・未ログイン・各受付不可状態を拒否。最後1枠へ10人同時要求し成功1件、DB人数/公開count/participant配列が一致。
4. **キャンセル（US4）**: 開始前の本人がfull/manual/cancelledから取り消せる。fullだけ自動受付、manual/cancelledは維持。再参加はOPENだけ。合流情報と本人一覧から消える。二重キャンセルで二重減算なし、他人の操作不可。
5. **管理/合流情報（US5）**: 募集者がFULLからmanual close、空きあり再開、中止。中止は不可逆・参加履歴維持。満員/manual中の合流情報更新、空/空白の未登録化、1000/1001境界。仕様の権限表全ケース、cancel後・開始後は参加者403、募集者は閲覧可だが編集不可。
6. **マイページ（US6）**: 自分の作成/参加を開始前/過去に区分、順序/次ページ/空を確認。満員/manual/cancelledも残る。キャンセル済み参加だけ消える。再読み込み・再ログイン・DB再接続で同じ結果。
7. **初期化（US7）**: contract DBへアカウント・募集・参加を作り、拡張SQLで初期化。同じIDで全工程を2回行う。ゲーム一覧は残る。開発DB名のガード拒否を専用fixtureで検証し、開発DBを実際に削除しない。

## 競合と時刻の専用テスト

- MutableClockでstartsAt直前/ちょうど/直後。中止を優先する状態、期限後すべての変更拒否、募集者private閲覧例外。
- ロックを保持して別要求を待機させ、Clockを開始時刻へ進めてから解除する。受信時点が開始前でも待機後の参加/キャンセル/管理は拒否。
- 最終検証前にClockを進め、作成の未来性が失われた場合はrollback、commit失敗503で親/参加を残さない。
- 参加対参加、同一本人対同一本人、キャンセル対キャンセル、キャンセル対close、reopen対join、cancel対meeting更新を実DBで確認。
- 公開detailの人数/配列一致、private読取対キャンセル/中止の順序、古い画面からの操作拒否。
- JWT失効/期限/CSRF/Origin/DB503、公開GETの不正JWT許容、機能ごとErrorのcode/fields、POST自動再送なしを回帰検証。

## 手動検証と記録

SC-001検索1分、SC-002登録〜公開5分、SC-003参加〜合流情報30秒、SC-006初見5人中4人は実測する。
未実施は未実施として記録。自動テスト時間や設計完成を達成結果に置き換えない。
SC-004/005/007は上の実DB・境界・初期化結果を記録する。
契約の構文/生成、実APIの契約、業務動作、手動受け入れを区別する。現時点で掲示板実装のテスト結果はない。


## 設計時の検証結果（2026-10-10）

OpenAPI Generator validate、Schemathesis読込（12操作）、Draft 2020-12の23 examplesと境界/認証条件、API interface 5個＋model 12個の一時生成・Java 25コンパイルが成功。
生成物は/tmpのみ。Maven統合・実API・全体の業務動作はまだ検証していない。
新FKに合わせるApiTestSupportの初期化とprivate行ロックtransactionのreadOnly=falseも実装対象。
