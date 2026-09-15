# backAndFrontDemo

Spring Boot + Vue3 全栈 Demo：用户注册/登录（图形验证码 + Redis）、国密 SM2 JWT（含自动续期）、用户分页与批量删除。

## 技术栈

| 端 | 技术 |
| --- | --- |
| 后端 | Java 17+、Spring Boot 3.5、MyBatis-Plus、Flyway、Redisson、Hutool SM2/验证码、BCrypt |
| 前端 | Vue 3、Vite、TypeScript、Axios、Element Plus、Vue Router |
| 基础设施 | MySQL 8、Redis（Docker Compose） |

## 环境要求

- JDK 17+
- Maven 3.9+（或 `backend/mvnw`）
- Node.js 18+（推荐 pnpm / npm）
- Docker / Docker Compose

## 1. 启动基础设施

```bash
docker compose up -d
```

| 服务 | 地址 | 账号 |
| --- | --- | --- |
| MySQL | `localhost:3306`，库名 `demo` | `demo` / `demo123` |
| Redis | `localhost:6379` | 无密码 |

## 2. 启动后端

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
- 公开接口：`/api/health`、`/api/auth/captcha`、`/api/auth/login`、`/api/auth/register`
- 需登录：`/api/users`、`/api/auth/me`（Header：`Authorization: Bearer <token>`）
- Token 临近过期时响应头返回 `X-New-Token` 用于续期

## 3. 启动前端

```bash
cd frontend
pnpm install   # 或 npm install
pnpm dev       # 或 npm run dev
```

- 访问：http://localhost:5173
- 页面：登录、注册、用户列表（分页 / 批量删除）

## 4. 联调建议

1. 打开注册页创建用户（需图形验证码）
2. 登录后进入用户列表
3. 勾选用户可批量删除

## 后端关键设计

- **过滤器** `JwtAuthFilter`：解析 JWT，写入 ThreadLocal，必要时写 `X-New-Token`
- **拦截器** `AuthInterceptor`：白名单外必须登录
- **AOP** `@OperLog`：记录用户操作日志
- **全局异常** `GlobalExceptionHandler` + `BizException`
- **参数校验** Jakarta Validation（`@Valid` / `@NotBlank` 等）
- **当前用户** `SecurityUtils.getCurrentUser()`
- **密码** BCrypt 加密落库
- **JWT** 国密 SM2 签名，claims 含 `userId`、`username`
