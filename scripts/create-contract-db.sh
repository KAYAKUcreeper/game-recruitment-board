#!/usr/bin/env bash
set -euo pipefail
TASK_ROOT=$(cd -- "$(dirname -- "$0")/.." && pwd)
cd "$TASK_ROOT"
docker compose exec -T db sh -c 'psql -U "$POSTGRES_USER" -d postgres -v ON_ERROR_STOP=1' <<'SQL'
SELECT 'CREATE DATABASE game_recruitment_contract'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'game_recruitment_contract');
\gexec
SQL
