#!/usr/bin/env bash
# 本机只构建并打包 demo-backend（测试开放 API，不含 mysql/redis）
# 默认打 linux/amd64，避免 Apple Silicon 镜像在 x86 服务器上 exec format error
# 产物：deploy/demo-images.tar
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TAG="${BACKEND_IMAGE_TAG:-demo-backend:0.0.1}"
OUT="${1:-$ROOT/deploy/demo-images.tar}"
PLATFORM="${DOCKER_PLATFORM:-linux/amd64}"

echo "==> build $TAG ($PLATFORM)"
docker build --platform "$PLATFORM" -t "$TAG" "$ROOT/backend"

echo "==> save -> $OUT"
mkdir -p "$(dirname "$OUT")"
docker save -o "$OUT" "$TAG"
ls -lh "$OUT"
echo "架构: $PLATFORM （服务器可用 uname -m 核对：x86_64 对应 amd64）"
echo "下一步：scp 到服务器后使用 docker-compose（有横杠）"
echo "  docker load -i demo-images.tar"
echo "  docker-compose up -d"
