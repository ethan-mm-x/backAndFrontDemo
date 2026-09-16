# 服务器部署包（无源码）— 开放 API 测试版

只打包 **backend** 镜像，**不含 MySQL / Redis**。适合把 `/api/open/**` 给别人调着用。

容器内用：

- 内嵌 **H2** 内存库（启动用）
- **内存 Nonce**（防重放，单机足够）

| 文件 | 说明 |
| --- | --- |
| `demo-images.tar` | 仅 `demo-backend:0.0.1`，本机打包产物，勿提交 Git |
| `docker-compose.yml` | 单个 `backend` 服务，`SPRING_PROFILES_ACTIVE=openapi` |
| `.env.example` | 复制为 `.env`，填 AK/SK |
| `load-and-up.sh` | `docker load` + `compose up` |

---

## 一、本机每次打包

仓库根目录：

```bash
chmod +x scripts/pack-images.sh
./scripts/pack-images.sh
```

生成 `deploy/demo-images.tar`（通常几十～一两百 MB 级，远小于带 MySQL 的 1GB+）。

改了后端代码 / Dockerfile 才需要重新打包；只改服务器 `.env` 不用重打。

---

## 二、拷到服务器

```text
demo-images.tar
docker-compose.yml
.env.example
load-and-up.sh
```

不要拷 `backend/src`。

```bash
scp deploy/demo-images.tar deploy/docker-compose.yml deploy/.env.example deploy/load-and-up.sh \
  user@SERVER:/opt/demo/
```

---

## 三、服务器操作

```bash
cd /opt/demo
cp .env.example .env
vim .env   # 设置 AKSK_SECRET_KEY（与调用方 SK 一致）

chmod +x load-and-up.sh
./load-and-up.sh
```

验活：

```bash
curl http://127.0.0.1:8080/api/health
# 期望含 "mode":"openapi-lite" 或 status=ok
```

测开放接口（本机）：

```bash
./scripts/aksk-demo.sh http://<服务器IP>:8080 <AK> <SK>
```

防火墙只开 **8080**。

| 操作 | 命令 |
| --- | --- |
| 状态 | `docker compose ps` |
| 日志 | `docker compose logs -f backend` |
| 停 | `docker compose down` |
| 换新镜像 | 覆盖 tar → `docker load -i demo-images.tar` → `docker compose up -d` |

---

## 四、给调用方

- Base URL：`http://<服务器IP>:8080`
- `GET /api/open/echo`、`POST /api/open/message`
- AK / SK：与服务器 `.env` 一致
- Maven：`com.mfx:mfx-spring-boot-starter:0.0.1-SNAPSHOT`

---

## 说明

- 本模式**不适合**生产多实例（Nonce 在内存，重启丢失）。
- 本地完整开发（用户登录 + MySQL + Redis）仍用仓库根目录 `docker-compose.yml`。
