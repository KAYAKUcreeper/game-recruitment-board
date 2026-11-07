# auth API契約テスト

テスト用DBへ接続した`contract`プロファイルのAPIに対して実行する。既定URLは
`http://127.0.0.1:8081/api`。開発DBへ向けないこと。

```bash
uv sync --project tests/contract --locked
uv run --project tests/contract pytest tests/contract/test_auth_contract.py
```

正常な5操作のシナリオと異常系を明示的に確認し、さらにSchemathesisが生成した入力で
応答を検証する。生成テストのseedは20261010、max_examplesは20。
Cookie/CSRFはfixtureで準備し、ケースごとに別のアカウントを使用する。
