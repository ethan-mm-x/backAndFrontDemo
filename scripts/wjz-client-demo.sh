#!/usr/bin/env bash
# 本机启动 backend（openapi + wjz），用对方 com.wjz SignApiService 调 HTTPS API
# 依赖：公司 Nexus 可拉 wjz-aksk-spring-boot-starter（本机 ~/.m2/settings.xml 需有账号）
#
# 注意：若本机开了 HTTPS 代理，8443 的 CONNECT 常被拦（403/Connection reset）。
# 本脚本默认对内网地址绕过代理。
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/backend"

export NO_PROXY="${NO_PROXY:-127.0.0.1,localhost,172.16.0.0/12,10.0.0.0/8}"
export no_proxy="$NO_PROXY"

echo "==> 启动：profiles=openapi,wjz  profile=-Pwjz-client  port=8080"
echo "    另开终端: curl http://127.0.0.1:8080/demo/ping"
echo "             curl -X POST 'http://127.0.0.1:8080/demo/echo?message=hi'"
exec ./mvnw -Pwjz-client spring-boot:run \
  -Dspring-boot.run.profiles=openapi,wjz
