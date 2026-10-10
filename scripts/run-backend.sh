#!/usr/bin/env bash
set -euo pipefail
TASK_ROOT=$(cd -- "$(dirname -- "$0")/.." && pwd)
cd "$TASK_ROOT"
if [ ! -f .env ]; then echo '.env.exampleを.envへコピーし、DBパスワードとJWT鍵を設定してください。' >&2; exit 1; fi
set -a
. ./.env
set +a
if [ -d "$TASK_ROOT/.tools/jdk-25.0.4.1+1" ]; then
  export JAVA_HOME="$TASK_ROOT/.tools/jdk-25.0.4.1+1"
  export PATH="$JAVA_HOME/bin:$PATH"
fi
export MAVEN_USER_HOME="$TASK_ROOT/.tools/maven-user"
profile=${1:-local}
if [ "$profile" = contract ]; then export DB_URL=jdbc:postgresql://localhost:5432/game_recruitment_contract; fi
exec ./backend/mvnw -f backend/pom.xml -Dmaven.repo.local="$TASK_ROOT/.tools/m2" spring-boot:run -Dspring-boot.run.profiles="$profile"
