# backAndFrontDemo

前后端空骨架：Spring Boot + Vue3，依赖与配置就绪，带一条健康检查用于联调。

## 技术栈

| 端 | 技术 |
| --- | --- |
| 后端 | Java 17+、Spring Boot 3.5、MyBatis-Plus、Flyway、JWT（工具类）、Redisson |
| 前端 | Vue 3、Vite、TypeScript、Axios、Element Plus |
| 基础设施 | MySQL 8、Redis（Docker Compose） |

## 环境要求

- JDK 17+（推荐 17 或 21；本机也可用更高版本）
- Maven 3.9+，或直接使用 `backend/mvnw`
- Node.js 18+
- Docker / Docker Compose

## 1. 启动基础设施

在仓库根目录执行：

```bash
docker compose up -d
```

| 服务 | 地址 | 账号 |
| --- | --- | --- |
| MySQL | `localhost:3306`，库名 `demo` | `demo` / `demo123`（root 密码 `root123`） |
| Redis | `localhost:6379` | 无密码 |

停止：

```bash
docker compose down
```

## 2. 启动后端

```bash
cd backend
./mvnw spring-boot:run
```

若本机已安装 Maven，也可：

```bash
cd backend
mvn -s .mvn/settings.xml spring-boot:run
```

> 项目自带 `.mvn/settings.xml`，走 Maven Central，避免本机私有 Nexus 拉不到依赖。`./mvnw` 会自动使用该配置。

- 服务地址：http://localhost:8080
- 健康检查：`GET http://localhost:8080/api/health`

成功时大致返回：

```json
{ "status": "ok", "redis": "PONG" }
```

## 3. 启动前端

另开一个终端：

```bash
cd frontend
npm install
npm run dev
```

- 访问：http://localhost:5173
- `/api` 已代理到 `http://localhost:8080`

## 4. 联调验证

1. 确保 MySQL、Redis、后端、前端均已启动
2. 打开 http://localhost:5173
3. 点击「健康检查」按钮，页面应显示 `ok`

## 目录结构

```
backAndFrontDemo/
  docker-compose.yml
  README.md
  backend/          # Spring Boot
  frontend/         # Vite + Vue3
```

## 说明

- JWT 仅提供 `JwtUtil` 与配置，未加登录接口与拦截器
- Flyway 脚本 `V1__init.sql` 为占位，未建业务表
- 本地开发通过 Vite 代理访问后端，无需额外 CORS 配置
