# backAndFrontDemo

Spring Boot + Vue3 全栈 Demo：用户注册/登录（图形验证码 + Redis）、国密 SM2 JWT（含自动续期）、用户分页与批量删除、开放 API 的国密 AK/SK（HMAC-SM3）鉴权示例。

## 技术栈

| 端 | 技术 |
| --- | --- |
| 后端 | Java 17+、Spring Boot 3.5、MyBatis-Plus、Flyway、Redisson、Hutool SM2/SM3/验证码、BCrypt |
| 前端 | Vue 3、Vite、TypeScript、Axios、Element Plus、Vue Router |
| 基础设施 | MySQL 8、Redis、可选后端镜像（Docker Compose） |

## 环境要求

- JDK 17+
- Maven 3.9+（或 `backend/mvnw`）
- Node.js 18+（推荐 pnpm / npm）
- Docker / Docker Compose

## 1. 启动基础设施

仅 MySQL + Redis（本地用 IDE / Maven 跑后端时）：

```bash
docker compose up -d mysql redis
```

| 服务 | 地址 | 账号 |
| --- | --- | --- |
| MySQL | `localhost:3306`，库名 `demo` | `demo` / `demo123` |
| Redis | `localhost:6379` | 无密码 |

## 2. 启动后端（本地开发）

```bash
cd backend
./mvnw spring-boot:run
```

或：

```bash
cd backend
mvn -s .mvn/settings.xml spring-boot:run
```

- 服务：http://localhost:8080
- 公开接口：`/api/health`、`/api/auth/public-key`、`/api/auth/captcha`、`/api/auth/login`、`/api/auth/register`
- 需登录：`/api/users`、`/api/auth/me`（Header：`Authorization: Bearer <token>`）
- 开放 API（AK/SK）：`/api/open/echo`（GET）、`/api/open/message`（POST）
- Token 临近过期时响应头返回 `X-New-Token` 用于续期
- 登录/注册的 `password` 为 SM2 密文；落库仍为 BCrypt

## 3. 启动前端

```bash
cd frontend
pnpm install   # 或 npm install
pnpm dev       # 或 npm run dev
```

- 访问：http://localhost:5173
- 页面：登录、注册、用户列表（分页 / 批量删除）

## 4. 国密 AK/SK 开放 API 示例

与登录 JWT 正交：调用方用 **AccessKey + SecretKey**，以国密 **HMAC-SM3** 对请求签名；**SK 不落网**，只传 AK、时间戳、Nonce、签名。

### 鉴权头

| Header | 说明 |
| --- | --- |
| `X-Access-Key` | AK，Demo 默认 `demo-ak-001` |
| `X-Timestamp` | Unix 秒级时间戳 |
| `X-Nonce` | 随机串，同一 AK 下不可重复（Redis 防重放） |
| `X-Signature` | `HMAC-SM3(SK, stringToSign)` 的 hex |

### 待签串

```text
METHOD\n
PATH\n
canonicalQuery\n
timestamp\n
nonce\n
sm3Hex(body)
```

- `PATH`：不含 query，例如 `/api/open/echo`
- `canonicalQuery`：query 按 key 排序后的 `k=v&...`；无 query 则为空串
- `body`：原始字节；GET 无 body 时按空字节做 SM3

Demo 密钥见 `backend/src/main/resources/application.yml` 的 `aksk.clients`。

### 一键调用 Demo

先启动后端，再执行：

```bash
chmod +x scripts/aksk-demo.sh
./scripts/aksk-demo.sh
# 或指定地址与密钥：
# ./scripts/aksk-demo.sh http://localhost:8080 demo-ak-001 demo-sk-please-change-me
```

等价 Maven 命令：

```bash
cd backend
./mvnw -q exec:java \
  -Dexec.classpathScope=compile \
  -Dexec.mainClass=com.demo.aksk.AkSkClientDemo \
  -Dexec.args="http://localhost:8080 demo-ak-001 demo-sk-please-change-me"
```

成功时 GET / POST 都会返回 `code: 0` 的 JSON。

### 接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/open/echo?name=world` | 回显参数与调用方名称 |
| POST | `/api/open/message` | Body：`{"title":"...","content":"..."}` |

概念说明见 [backend/ak和sk.md](backend/ak和sk.md)。

## 5. mfx Spring Boot Starter（调用方 SDK）

仓库模块 [`mfx-spring-boot-starter`](mfx-spring-boot-starter/)：基于 **Spring Boot 3.2.4**，封装与上面约定一致的 AK/SK HTTP 客户端，使用方注入 `MfxApiService` 即可 `get` / `post`。

**Maven 坐标**：`com.mfx:mfx-spring-boot-starter:0.0.1-SNAPSHOT`  
已发布到公司 Nexus Snapshots（`http://10.126.138.142:8081/nexus/content/repositories/snapshots`），他人可直接依赖拉取；仓库与鉴权配置见 [mfx-spring-boot-starter/README.md](mfx-spring-boot-starter/README.md)。

本地安装：

```bash
cd mfx-spring-boot-starter && mvn -s .mvn/settings.xml -q clean install
```

使用方配置：

```yaml
mfx:
  base-url: http://localhost:8080
  access-key: demo-ak-001
  secret-key: demo-sk-please-change-me
```

```java
@Autowired
private MfxApiService mfxApiService;

mfxApiService.get("/api/open/echo", Map.of("name", "world"));
mfxApiService.post("/api/open/message", "{\"title\":\"ping\",\"content\":\"from-mfx\"}");
```

完整对接文档（坐标、Nexus 仓库、发布步骤）见 [mfx-spring-boot-starter/README.md](mfx-spring-boot-starter/README.md)。

## 6. 无源码部署到服务器（镜像包）

服务器**不要拷源码**，也不要 `docker compose --build`。本机打镜像 tar，服务器 `docker load`。

### 本机打包

```bash
chmod +x scripts/pack-images.sh deploy/load-and-up.sh
./scripts/pack-images.sh
```

生成 `deploy/demo-images.tar`（含 `demo-backend:0.0.1`、`mysql:8.0`、`redis:7`）。

拷到服务器（仅这些文件，不要拷 `backend/src`）：

- `deploy/demo-images.tar`
- `deploy/docker-compose.yml`
- `deploy/.env.example`（或已填好的 `.env`）
- `deploy/load-and-up.sh`

```bash
scp deploy/demo-images.tar deploy/docker-compose.yml deploy/.env.example deploy/load-and-up.sh user@SERVER:/opt/demo/
```

### 服务器启动

```bash
cd /opt/demo
chmod +x load-and-up.sh
./load-and-up.sh
# 或：docker load -i demo-images.tar && cp .env.example .env && vim .env && docker compose up -d
```

- 防火墙只开 **8080**
- MySQL / Redis **不**映射到宿主机
- 改 `.env` 里的 `AKSK_SECRET_KEY`、`MYSQL_PASSWORD`（与调用方 SDK 的 SK 一致）

验活：

```bash
curl http://127.0.0.1:8080/api/health
# 本机对服务器
./scripts/aksk-demo.sh http://<服务器IP>:8080 <AK> <SK>
```

本地开发仍可：`docker compose up -d mysql redis`，后端用 Maven 跑。根目录 compose 仅用于开发构建，不要拿到服务器去 `--build`。

### 给调用方

- Base URL：`http://<服务器IP>:8080`
- 路径：`GET /api/open/echo`、`POST /api/open/message`
- Maven：`com.mfx:mfx-spring-boot-starter:0.0.1-SNAPSHOT`

## 7. 联调建议

1. 打开注册页创建用户（需图形验证码）
2. 登录后进入用户列表
3. 勾选用户可批量删除
4. 用 `scripts/aksk-demo.sh` 或 `MfxApiService` 验证开放 API 签名

## 后端关键设计

详细目录、请求链路、接口清单与学习阅读顺序见 **[backend/README.md](backend/README.md)**。

- **过滤器** `JwtAuthFilter`：解析 JWT，写入 ThreadLocal，必要时写 `X-New-Token`
- **过滤器** `AkSkAuthFilter`：`/api/open/**` 上做 HMAC-SM3 验签 + 时间窗 + Nonce
- **拦截器** `AuthInterceptor`：白名单 / 开放 API 前缀外必须登录
- **AOP** `@OperLog`：记录用户操作日志
- **全局异常** `GlobalExceptionHandler` + `BizException`
- **参数校验** Jakarta Validation（`@Valid` / `@NotBlank` 等）
- **当前用户** `SecurityUtils.getCurrentUser()`
- **密码** 传输 SM2 加密，落库 BCrypt
- **JWT** 国密 SM2 签名，claims 含 `userId`、`username`
- **开放 API** 国密 HMAC-SM3 的 AK/SK 签名
- **调用方 SDK** `mfx-spring-boot-starter`：自动签名的 `MfxApiService`
