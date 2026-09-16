# mfx-spring-boot-starter

Spring Boot **3.2.4** Starter：封装符合本仓库开放 API 约定的 **国密 AK/SK（HMAC-SM3）** HTTP 客户端，提供可注入的 `MfxApiService`。

**给对接方看的调用说明（GET / POST）：** 见 **[使用说明.md](使用说明.md)**。

## Maven 坐标

| 项 | 值 |
| --- | --- |
| groupId | `com.mfx` |
| artifactId | `mfx-spring-boot-starter` |
| version | `0.0.1-SNAPSHOT` |
| 仓库 | 公司 Nexus Snapshots |

Nexus Browse 路径示例：`Snapshots/com/mfx/mfx-spring-boot-starter/0.0.1-SNAPSHOT/`

仓库地址：

```text
http://10.126.138.142:8081/nexus/content/repositories/snapshots
```

## 使用方接入

### 1. 配置仓库

在使用方 `pom.xml`（或父 POM）中增加 snapshots 仓库：

```xml
<repositories>
  <repository>
    <id>nexus-snapshots</id>
    <url>http://10.126.138.142:8081/nexus/content/repositories/snapshots</url>
    <snapshots>
      <enabled>true</enabled>
    </snapshots>
    <releases>
      <enabled>false</enabled>
    </releases>
  </repository>
</repositories>
```

若公司 Nexus 拉取也需要账号，在本机 `~/.m2/settings.xml` 增加（**勿把真实密码提交到 Git**）：

```xml
<servers>
  <server>
    <id>nexus-snapshots</id>
    <username>YOUR_USERNAME</username>
    <password>YOUR_PASSWORD</password>
  </server>
</servers>
```

`<server><id>` 必须与上面 `<repository><id>` 一致。

### 2. 依赖

```xml
<dependency>
  <groupId>com.mfx</groupId>
  <artifactId>mfx-spring-boot-starter</artifactId>
  <version>0.0.1-SNAPSHOT</version>
</dependency>
```

使用方 Spring Boot 建议 **3.2.x**（与本 Starter 对齐）；更高 minor 一般也可，需自行验证。

### 3. 配置

```yaml
mfx:
  enabled: true
  base-url: http://localhost:8080
  access-key: demo-ak-001
  secret-key: demo-sk-please-change-me
  connect-timeout: 5s
  read-timeout: 10s
```

`access-key` / `secret-key` 须与开放 API 服务端 `aksk.clients` 一致。关闭：`mfx.enabled=false`。

### 4. 注入调用

```java
@Service
public class OpenApiCaller {
    private final MfxApiService mfxApiService;

    public OpenApiCaller(MfxApiService mfxApiService) {
        this.mfxApiService = mfxApiService;
    }

    public void demo() {
        // GET /api/open/echo?name=world
        String echo = mfxApiService.get("/api/open/echo", Map.of("name", "world"));

        // POST /api/open/message
        String json = "{\"title\":\"ping\",\"content\":\"from-mfx-sdk\"}";
        String created = mfxApiService.post("/api/open/message", json);

        // 或反序列化到 Map / 自定义 DTO
        Map<?, ?> body = mfxApiService.post(
                "/api/open/message",
                Map.of("title", "ping", "content", "dto"),
                Map.class
        );
    }
}
```

## 维护者：发布到 Nexus

1. 复制部署 settings（含账号，已 gitignore）：

```bash
cd mfx-spring-boot-starter
cp .mvn/settings-deploy.xml.example .mvn/settings-deploy.xml
# 编辑 settings-deploy.xml，填入 Nexus 密码
```

2. **推荐**使用仓库脚本发布（对 Nexus 2.x 更稳，避免 `mvn deploy` 的 `NoHttpResponseException`）：

```bash
# 在仓库根目录
./scripts/deploy-mfx-nexus.sh
```

或尝试 Maven 原生部署（部分网络环境下可能失败）：

```bash
cd mfx-spring-boot-starter
mvn -s .mvn/settings-deploy.xml clean deploy -DskipTests
```

不要用带 `mirrorOf=*` 且未排除 `nexus-snapshots` 的 settings 做 deploy。

3. 在 Nexus 界面确认：`Snapshots` → Browse Storage → `com/mfx/mfx-spring-boot-starter/`

本地仅安装（不推 Nexus）仍可用：

```bash
mvn -s .mvn/settings.xml -q clean install
```

## 签名约定

与服务端 [`backend/.../AkSkSigner`](../backend/src/main/java/com/demo/aksk/AkSkSigner.java) 一致：

- Header：`X-Access-Key` / `X-Timestamp` / `X-Nonce` / `X-Signature`
- 待签串：`METHOD\nPATH\ncanonicalQuery\ntimestamp\nnonce\nsm3Hex(body)`
- 算法：`HMAC-SM3(SK, stringToSign)`

**SK 永不上传**，只存在于调用方配置与服务端。

## 联调

1. 启动本仓库 backend（含 MySQL / Redis）
2. 使用方配置 Demo AK/SK 后调用上述接口
3. 也可对照仓库根目录 `./scripts/aksk-demo.sh`（Java Demo 客户端，非本 Starter）

## 模块说明

| 类 | 职责 |
| --- | --- |
| `AkSkSigner` | 签名算法 |
| `MfxHttpClient` | RestClient + 算签发请求 |
| `MfxApiService` | 使用方 API |
| `MfxAutoConfiguration` | 自动装配 |

运行单测：

```bash
mvn -s .mvn/settings.xml -q test
```
