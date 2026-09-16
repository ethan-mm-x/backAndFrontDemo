# 服务器部署包（无源码）

本目录给 **本机打包镜像 → 服务器 load 后启动** 用，服务器上**不要**放 `backend/src` 等源码。

| 文件 | 说明 |
| --- | --- |
| `demo-images.tar` | 本机打包产物（含 backend / mysql / redis 镜像），**勿提交 Git** |
| `docker-compose.yml` | 只有 `image:`，无 `build:`，不映射 MySQL/Redis 端口 |
| `.env.example` | 环境变量模板，复制为 `.env` 后改密钥 |
| `load-and-up.sh` | 服务器一键：`docker load` + `compose up` |
| `.env` | 真实密钥（gitignore，勿提交） |

---

## 一、每次本机需要打包（有代码变更或首次上线）

在**有源码的开发机**上，仓库根目录执行：

```bash
chmod +x scripts/pack-images.sh
./scripts/pack-images.sh
```

脚本会：

1. `docker compose build backend` → 打出 `demo-backend:0.0.1`
2. `docker pull mysql:8.0`、`redis:7`
3. `docker save` 三个镜像到 `deploy/demo-images.tar`

可选：指定输出路径

```bash
./scripts/pack-images.sh /path/to/demo-images.tar
```

**何时需要重新打包？**

- 改了后端 Java / `application*.yml` / Dockerfile
- 想换 backend 镜像内容  
未改代码、只改服务器 `.env`（AK/SK、库密码）→ **不用重新打包**，改 `.env` 后 `docker compose up -d` 即可。

---

## 二、拷到服务器（只要这些）

把下面文件拷到服务器同一目录（例如 `/opt/demo/`），**不要**拷 `backend/src`：

```text
demo-images.tar
docker-compose.yml
.env.example
load-and-up.sh
```

示例：

```bash
scp deploy/demo-images.tar \
    deploy/docker-compose.yml \
    deploy/.env.example \
    deploy/load-and-up.sh \
    user@SERVER:/opt/demo/
```

---

## 三、服务器上需要的操作

### 1. 准备环境

- 已安装 Docker、Docker Compose 插件
- 防火墙 / 安全组只放行 **8080**（不要对公网开 3306、6379）

### 2. 配置密钥（首次必做）

```bash
cd /opt/demo
cp .env.example .env
vim .env   # 或 nano
```

至少改：

```bash
AKSK_ACCESS_KEY=你的AK
AKSK_SECRET_KEY=你的SK     # 与调用方 mfx SDK 的 secret-key 一致
AKSK_CLIENT_NAME=任意名称
MYSQL_PASSWORD=强密码       # 与库密码一致即可
MYSQL_ROOT_PASSWORD=强密码
```

### 3. 加载镜像并启动

```bash
chmod +x load-and-up.sh
./load-and-up.sh
```

等价手动步骤：

```bash
docker load -i demo-images.tar
docker compose up -d
```

### 4. 验活

```bash
curl http://127.0.0.1:8080/api/health
docker compose ps
docker compose logs -f backend   # 排错时用
```

本机对服务器测开放接口（SK 用 `.env` 里同一份）：

```bash
./scripts/aksk-demo.sh http://<服务器IP>:8080 <AK> <SK>
```

### 5. 日常运维

| 操作 | 命令 |
| --- | --- |
| 查看状态 | `docker compose ps` |
| 看日志 | `docker compose logs -f backend` |
| 停服务 | `docker compose down` |
| 只改 `.env` 后重启 | `docker compose up -d` |
| 换新镜像包 | 覆盖 `demo-images.tar` → `docker load -i demo-images.tar` → `docker compose up -d` |

---

## 四、给调用方的信息

- Base URL：`http://<服务器IP>:8080`
- 接口：`GET /api/open/echo`、`POST /api/open/message`
- AK / SK：与服务器 `.env` 一致
- Maven：`com.mfx:mfx-spring-boot-starter:0.0.1-SNAPSHOT`
