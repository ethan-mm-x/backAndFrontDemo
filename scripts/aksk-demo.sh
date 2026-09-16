#!/usr/bin/env bash
# 调用国密 AK/SK 开放接口 Demo（依赖 backend 已启动）
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BASE_URL="${1:-http://172.16.22.148:8080}"
AK="${2:-demo-ak-001}"
SK="${3:-a8f3K9mQx2Vp7nR4wYcE6uHt1bZs0Ld9}"

cd "$ROOT/backend"
./mvnw -q exec:java \
  -Dexec.classpathScope=compile \
  -Dexec.mainClass=com.demo.aksk.AkSkClientDemo \
  -Dexec.args="${BASE_URL} ${AK} ${SK}"
