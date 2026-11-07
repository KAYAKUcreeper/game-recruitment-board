#!/usr/bin/env bash
set -euo pipefail
TASK_ROOT=$(cd -- "$(dirname -- "$0")/.." && pwd)
cd "$TASK_ROOT"
docker compose exec -T db sh -c 'psql -U "$POSTGRES_USER" -d game_recruitment_contract -v ON_ERROR_STOP=1' < backend/src/test/resources/fixtures/reset_contract.sql
