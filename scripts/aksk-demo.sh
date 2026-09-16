#!/usr/bin/env bash
# 调用国密 AK/SK 开放接口 Demo（依赖 backend 已启动）
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BASE_URL="${1:-http://localhost:8080}"
AK="${2:-demo-ak-001}"
SK="${3:-demo-sk-please-change-me}"

cd "$ROOT/backend"
./mvnw -q exec:java \
  -Dexec.classpathScope=compile \
  -Dexec.mainClass=com.demo.aksk.AkSkClientDemo \
  -Dexec.args="${BASE_URL} ${AK} ${SK}"
