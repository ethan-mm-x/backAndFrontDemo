package com.demo.aksk;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

/**
 * 「调用方」Demo：模拟另一个服务用 AK/SK 调我们的开放 API。
 * <p>
 * 学习建议：<b>先读这个类，再读 Filter</b>。
 * 这里做的事，几乎就是你将来在 Node/Java 服务端写的「axios 请求拦截器」：
 * <ol>
 *   <li>拼 stringToSign</li>
 *   <li>用 SK 算 signature</li>
 *   <li>把 AK / 时间戳 / nonce / signature 放进 Header</li>
 *   <li>发 GET / POST</li>
 * </ol>
 * 运行方式（后端需已启动）：仓库根目录 {@code ./scripts/aksk-demo.sh}
 * 或 Maven：
 * <pre>
 *   ./mvnw -q exec:java -Dexec.mainClass=com.demo.aksk.AkSkClientDemo
 * </pre>
 * 可选参数：{@code baseUrl accessKey secretKey}
 * <p>
 * <b>安全提醒</b>：SK 可以出现在本 Demo / 服务端配置里，但不要写进浏览器前端代码。
 */
public final class AkSkClientDemo {

    private AkSkClientDemo() {
    }

    public static void main(String[] args) throws Exception {
        String baseUrl = args.length > 0 ? trimSlash(args[0]) : "http://localhost:8080";
        String accessKey = args.length > 1 ? args[1] : "demo-ak-001";
        String secretKey = args.length > 2 ? args[2] : "demo-sk-please-change-me";

        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

        // 1) GET：query 会进入签名
        System.out.println("=== GET /api/open/echo ===");
        call(client, "GET", baseUrl + "/api/open/echo?name=world", null, accessKey, secretKey);

        System.out.println();
        // 2) POST：JSON 原始字节会进入签名（先 SM3 再 HMAC）
        System.out.println("=== POST /api/open/message ===");
        String json = "{\"title\":\"ping\",\"content\":\"aksk-sm3-demo\"}";
        call(client, "POST", baseUrl + "/api/open/message", json, accessKey, secretKey);
    }

    /**
     * 单次请求：算签 → 塞 Header → 发送。
     * 打印 stringToSign / signature 是为了让你对照服务端规则排查。
     */
    private static void call(HttpClient client,
                             String method,
                             String url,
                             String body,
                             String accessKey,
                             String secretKey) throws Exception {
        URI uri = URI.create(url);
        String path = uri.getRawPath();
        String query = uri.getRawQuery();
        // GET 时 body 为空字节；POST 时必须与真正发送的字节一致
        byte[] bodyBytes = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000L);
        String nonce = UUID.randomUUID().toString().replace("-", "");

        String stringToSign = AkSkSigner.buildStringToSign(method, path, query, timestamp, nonce, bodyBytes);
        String signature = AkSkSigner.sign(secretKey, stringToSign);

        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(10))
                .header(AkSkAuthFilter.HEADER_ACCESS_KEY, accessKey)
                .header(AkSkAuthFilter.HEADER_TIMESTAMP, timestamp)
                .header(AkSkAuthFilter.HEADER_NONCE, nonce)
                .header(AkSkAuthFilter.HEADER_SIGNATURE, signature);

        if ("GET".equalsIgnoreCase(method)) {
            builder.GET();
        } else {
            builder.header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(bodyBytes));
        }

        HttpResponse<String> resp = client.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        System.out.println("stringToSign:");
        System.out.println(stringToSign.replace("\n", "\\n"));
        System.out.println("signature: " + signature);
        System.out.println("HTTP " + resp.statusCode());
        System.out.println(resp.body());
    }

    private static String trimSlash(String base) {
        if (base.endsWith("/")) {
            return base.substring(0, base.length() - 1);
        }
        return base;
    }
}
