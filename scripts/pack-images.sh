#!/usr/bin/env bash
# 本机只构建并打包 demo-backend（测试开放 API，不含 mysql/redis）
# 产物：deploy/demo-images.tar
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TAG="${BACKEND_IMAGE_TAG:-demo-backend:0.0.1}"
OUT="${1:-$ROOT/deploy/demo-images.tar}"

echo "==> build $TAG"
docker compose -f "$ROOT/docker-compose.yml" build backend

echo "==> save -> $OUT"
mkdir -p "$(dirname "$OUT")"
docker save -o "$OUT" "$TAG"
ls -lh "$OUT"
echo "下一步：scp 到服务器后"
echo "  docker load -i demo-images.tar"
echo "  cp .env.example .env && 编辑 AKSK_SECRET_KEY"
echo "  docker compose up -d"
