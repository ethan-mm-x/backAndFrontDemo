#!/usr/bin/env bash
# 服务器：加载镜像并启动（本目录需有 demo-images.tar 与 docker-compose.yml）
# 兼容 docker-compose（V2 独立二进制）与 docker compose 插件
set -euo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DIR"

if command -v docker-compose >/dev/null 2>&1; then
  COMPOSE=(docker-compose)
elif docker compose version >/dev/null 2>&1; then
  COMPOSE=(docker compose)
else
  echo "未找到 docker-compose / docker compose，请先安装"
  exit 1
fi

if [[ ! -f demo-images.tar ]]; then
  echo "缺少 demo-images.tar，请先在本机执行 ./scripts/pack-images.sh 再 scp 过来"
  exit 1
fi

docker load -i demo-images.tar
"${COMPOSE[@]}" up -d
echo "检查: curl http://127.0.0.1:8080/api/health"
echo "若仍失败: ${COMPOSE[*]} logs --tail=100"
