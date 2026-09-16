package com.mfx.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mfx.autoconfigure.MfxProperties;
import com.mfx.sign.AkSkHeaders;
import com.mfx.sign.AkSkSigner;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

/**
 * 带国密 AK/SK 签名的 HTTP 客户端（基于 Spring {@link RestClient}）。
 */
public class MfxHttpClient {

    private final RestClient restClient;
    private final MfxProperties properties;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public MfxHttpClient(MfxProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.baseUrl = trimSlash(properties.getBaseUrl());

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        if (properties.getConnectTimeout() != null) {
            factory.setConnectTimeout((int) properties.getConnectTimeout().toMillis());
        }
        if (properties.getReadTimeout() != null) {
            factory.setReadTimeout((int) properties.getReadTimeout().toMillis());
        }
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    public String get(String path, Map<String, ?> query) {
        return exchange(HttpMethod.GET, path, query, null, String.class);
    }

    public <T> T get(String path, Map<String, ?> query, Class<T> responseType) {
        return exchange(HttpMethod.GET, path, query, null, responseType);
    }

    public String post(String path, String jsonBody) {
        byte[] body = jsonBody == null ? new byte[0] : jsonBody.getBytes(StandardCharsets.UTF_8);
        return exchange(HttpMethod.POST, path, null, body, String.class);
    }

    public <T> T post(String path, Object body, Class<T> responseType) {
        byte[] bytes = serializeBody(body);
        return exchange(HttpMethod.POST, path, null, bytes, responseType);
    }

    private <T> T exchange(HttpMethod method,
                           String path,
                           Map<String, ?> query,
                           byte[] bodyBytes,
                           Class<T> responseType) {
        if (!StringUtils.hasText(path)) {
            throw new MfxApiException("path 不能为空");
        }
        byte[] payload = bodyBytes == null ? new byte[0] : bodyBytes;
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        String queryString = AkSkSigner.canonicalQueryFromMap(query);

        URI uri = UriComponentsBuilder
                .fromHttpUrl(baseUrl + normalizedPath)
                .query(queryString.isEmpty() ? null : queryString)
                .build(true)
                .toUri();

        String timestamp = String.valueOf(System.currentTimeMillis() / 1000L);
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String stringToSign = AkSkSigner.buildStringToSign(
                method.name(),
                uri.getRawPath(),
                uri.getRawQuery(),
                timestamp,
                nonce,
                payload
        );
        String signature = AkSkSigner.sign(properties.getSecretKey(), stringToSign);

        RestClient.RequestBodySpec spec = restClient.method(method)
                .uri(uri)
                .header(AkSkHeaders.ACCESS_KEY, properties.getAccessKey())
                .header(AkSkHeaders.TIMESTAMP, timestamp)
                .header(AkSkHeaders.NONCE, nonce)
                .header(AkSkHeaders.SIGNATURE, signature);

        if (method == HttpMethod.POST) {
            spec = spec.contentType(MediaType.APPLICATION_JSON)
                    .body(payload);
        }

        try {
            return spec.retrieve()
                    .onStatus(status -> status.value() < 200 || status.value() >= 300, (req, res) -> {
                        String errBody = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        String brief = errBody.length() > 512 ? errBody.substring(0, 512) + "..." : errBody;
                        throw new MfxApiException(
                                "HTTP " + res.getStatusCode().value() + ": " + brief,
                                res.getStatusCode().value(),
                                errBody
                        );
                    })
                    .body(responseType);
        } catch (MfxApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new MfxApiException("请求失败: " + ex.getMessage(), ex);
        }
    }

    private byte[] serializeBody(Object body) {
        if (body == null) {
            return new byte[0];
        }
        if (body instanceof byte[] bytes) {
            return bytes;
        }
        if (body instanceof String str) {
            return str.getBytes(StandardCharsets.UTF_8);
        }
        try {
            return objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            throw new MfxApiException("JSON 序列化失败: " + e.getMessage(), e);
        }
    }

    private static String trimSlash(String base) {
        if (base == null) {
            return "";
        }
        String s = base.trim();
        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }
}
