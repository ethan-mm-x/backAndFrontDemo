package com.demo.aksk;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

/**
 * 本地联调用的 AK/SK 客户端 Demo（国密 HMAC-SM3）。
 * <p>
 * 运行（需先启动后端）：
 * <pre>
 *   ./mvnw -q exec:java -Dexec.mainClass=com.demo.aksk.AkSkClientDemo
 * </pre>
 * 可选参数：
 * <pre>
 *   baseUrl accessKey secretKey
 * </pre>
 */
public final class AkSkClientDemo {

    private AkSkClientDemo() {
    }

    public static void main(String[] args) throws Exception {
        String baseUrl = args.length > 0 ? trimSlash(args[0]) : "http://localhost:8080";
        String accessKey = args.length > 1 ? args[1] : "demo-ak-001";
        String secretKey = args.length > 2 ? args[2] : "demo-sk-please-change-me";

        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

        System.out.println("=== GET /api/open/echo ===");
        call(client, "GET", baseUrl + "/api/open/echo?name=world", null, accessKey, secretKey);

        System.out.println();
        System.out.println("=== POST /api/open/message ===");
        String json = "{\"title\":\"ping\",\"content\":\"aksk-sm3-demo\"}";
        call(client, "POST", baseUrl + "/api/open/message", json, accessKey, secretKey);
    }

    private static void call(HttpClient client,
                             String method,
                             String url,
                             String body,
                             String accessKey,
                             String secretKey) throws Exception {
        URI uri = URI.create(url);
        String path = uri.getRawPath();
        String query = uri.getRawQuery();
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
