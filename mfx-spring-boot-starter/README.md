# mfx-spring-boot-starter

Spring Boot **3.2.4** Starter：封装符合本仓库开放 API 约定的 **国密 AK/SK（HMAC-SM3）** HTTP 客户端，提供可注入的 `MfxApiService`。

## 安装到本地仓库

```bash
cd mfx-spring-boot-starter
mvn -s .mvn/settings.xml -q clean install
```

## 使用方接入

### 1. 依赖

```xml
<dependency>
  <groupId>com.mfx</groupId>
  <artifactId>mfx-spring-boot-starter</artifactId>
  <version>0.0.1-SNAPSHOT</version>
</dependency>
```

使用方 Spring Boot 建议 3.2.x（与本 Starter 对齐）；更高 minor 一般也可，需自行验证。

### 2. 配置

```yaml
mfx:
  enabled: true
  base-url: http://localhost:8080
  access-key: demo-ak-001
  secret-key: demo-sk-please-change-me
  connect-timeout: 5s
  read-timeout: 10s
```

密钥须与服务端 `aksk.clients` 一致。关闭：`mfx.enabled=false`。

### 3. 注入调用

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
