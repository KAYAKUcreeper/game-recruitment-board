#!/usr/bin/env bash
set -euo pipefail
TASK_ROOT=$(cd -- "$(dirname -- "$0")/.." && pwd)
cd "$TASK_ROOT"
python3 - <<'PY'
from pathlib import Path
import base64
import secrets
path = Path('.env')
if path.exists():
    print('.envは既に存在するため、変更していません。')
else:
    path.write_text('DB_URL=jdbc:postgresql://localhost:5432/game_recruitment\nDB_USERNAME=game_recruitment\nDB_PASSWORD=' + secrets.token_urlsafe(32) + '\nAPP_JWT_SECRET_BASE64=' + base64.b64encode(secrets.token_bytes(32)).decode() + '\n')
    path.chmod(0o600)
    print('開発用.envを作成しました。秘密値は表示しません。')
PY
