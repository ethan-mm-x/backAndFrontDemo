#!/usr/bin/env bash
# 服务器：加载镜像并启动（本目录需有 demo-images.tar 与 docker-compose.yml）
set -euo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DIR"
if [[ ! -f demo-images.tar ]]; then
  echo "缺少 demo-images.tar，请先在本机执行 ./scripts/pack-images.sh 再 scp 过来"
  exit 1
fi
if [[ ! -f .env ]]; then
  cp .env.example .env
  echo "已生成 .env，请修改 AKSK_SECRET_KEY / 数据库密码后再启动"
fi
docker load -i demo-images.tar
docker compose up -d
echo "检查: curl http://127.0.0.1:8080/api/health"
