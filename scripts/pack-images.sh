#!/usr/bin/env bash
# 本机构建 backend 镜像，并与 mysql/redis 一起 docker save。
# 产物：deploy/demo-images.tar（不要提交）
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TAG="${BACKEND_IMAGE_TAG:-demo-backend:0.0.1}"
OUT="${1:-$ROOT/deploy/demo-images.tar}"

echo "==> build $TAG"
docker compose -f "$ROOT/docker-compose.yml" build backend
docker pull mysql:8.0
docker pull redis:7

echo "==> save -> $OUT"
mkdir -p "$(dirname "$OUT")"
docker save -o "$OUT" "$TAG" mysql:8.0 redis:7
ls -lh "$OUT"
echo "下一步：把 $OUT 与 deploy/docker-compose.yml、deploy/.env.example 拷到服务器"
echo "  docker load -i demo-images.tar"
echo "  cp .env.example .env && 编辑密钥"
echo "  docker compose up -d"
