#!/usr/bin/env bash
set -euo pipefail
TASK_ROOT=$(cd -- "$(dirname -- "$0")/.." && pwd)
cd "$TASK_ROOT"
if [ -x .tools/jdk-25.0.4.1+1/bin/java ]; then .tools/jdk-25.0.4.1+1/bin/java -version; exit 0; fi
mkdir -p .tools
curl -fL 'https://github.com/adoptium/temurin25-binaries/releases/download/jdk-25.0.4.1%2B1/OpenJDK25U-jdk_x64_linux_hotspot_25.0.4.1_1.tar.gz' -o .tools/temurin25.tar.gz
printf '%s\n' 'dbb698396d478e7fa2b1e50f4103324b2a99b90569ee27c33f2261f9215cf41e  .tools/temurin25.tar.gz' | sha256sum -c -
tar -xzf .tools/temurin25.tar.gz -C .tools
.tools/jdk-25.0.4.1+1/bin/java -version
