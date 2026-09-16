package com.demo.aksk;

import com.demo.common.ApiResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * AK/SK 验签过滤器：仅拦截 {@code /api/open/**}。
 * <p>
 * Header：
 * <ul>
 *   <li>{@code X-Access-Key}</li>
 *   <li>{@code X-Timestamp}（Unix 秒）</li>
 *   <li>{@code X-Nonce}</li>
 *   <li>{@code X-Signature}（HMAC-SM3 hex）</li>
 * </ul>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AkSkAuthFilter extends OncePerRequestFilter {

    public static final String HEADER_ACCESS_KEY = "X-Access-Key";
    public static final String HEADER_TIMESTAMP = "X-Timestamp";
    public static final String HEADER_NONCE = "X-Nonce";
    public static final String HEADER_SIGNATURE = "X-Signature";
    public static final String ATTR_CLIENT_NAME = "aksk.clientName";

    private final AkSkProperties properties;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;
    private final Map<String, AkSkProperties.Client> clientIndex = new ConcurrentHashMap<>();

    public AkSkAuthFilter(AkSkProperties properties, RedissonClient redissonClient, ObjectMapper objectMapper) {
        this.properties = properties;
        this.redissonClient = redissonClient;
        this.objectMapper = objectMapper;
        rebuildIndex();
    }

    private void rebuildIndex() {
        clientIndex.clear();
        clientIndex.putAll(properties.getClients().stream()
                .filter(c -> StringUtils.hasText(c.getAccessKey()))
                .collect(Collectors.toMap(AkSkProperties.Client::getAccessKey, Function.identity(), (a, b) -> a)));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/api/open/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        CachedBodyHttpServletRequest wrapped = new CachedBodyHttpServletRequest(request);
        try {
            verify(wrapped);
            filterChain.doFilter(wrapped, response);
        } catch (AkSkAuthException ex) {
            writeFail(response, ex.getCode(), ex.getMessage());
        }
    }

    private void verify(CachedBodyHttpServletRequest request) {
        if (clientIndex.isEmpty()) {
            rebuildIndex();
        }

        String accessKey = request.getHeader(HEADER_ACCESS_KEY);
        String timestamp = request.getHeader(HEADER_TIMESTAMP);
        String nonce = request.getHeader(HEADER_NONCE);
        String signature = request.getHeader(HEADER_SIGNATURE);

        if (!StringUtils.hasText(accessKey) || !StringUtils.hasText(timestamp)
                || !StringUtils.hasText(nonce) || !StringUtils.hasText(signature)) {
            throw new AkSkAuthException(401, "缺少 AK/SK 鉴权头");
        }

        AkSkProperties.Client client = clientIndex.get(accessKey);
        if (client == null || !StringUtils.hasText(client.getSecretKey())) {
            throw new AkSkAuthException(401, "无效的 AccessKey");
        }

        long ts;
        try {
            ts = Long.parseLong(timestamp.trim());
        } catch (NumberFormatException e) {
            throw new AkSkAuthException(401, "时间戳格式错误");
        }
        long now = System.currentTimeMillis() / 1000L;
        if (Math.abs(now - ts) > properties.getSkewSeconds()) {
            throw new AkSkAuthException(401, "请求已过期或时间偏差过大");
        }

        String nonceKey = "aksk:nonce:" + accessKey + ":" + nonce;
        RBucket<String> bucket = redissonClient.getBucket(nonceKey);
        boolean firstUse = bucket.setIfAbsent("1", Duration.ofSeconds(properties.getSkewSeconds()));
        if (!firstUse) {
            throw new AkSkAuthException(401, "Nonce 已使用，疑似重放");
        }

        String stringToSign = AkSkSigner.buildStringToSign(
                request.getMethod(),
                request.getRequestURI(),
                request.getQueryString(),
                timestamp.trim(),
                nonce.trim(),
                request.getCachedBody()
        );
        String expected = AkSkSigner.sign(client.getSecretKey(), stringToSign);
        if (!AkSkSigner.matches(expected, signature.trim())) {
            throw new AkSkAuthException(401, "签名校验失败");
        }

        request.setAttribute(ATTR_CLIENT_NAME,
                StringUtils.hasText(client.getName()) ? client.getName() : accessKey);
    }

    private void writeFail(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResult.fail(code, message)));
    }

    private static final class AkSkAuthException extends RuntimeException {
        private final int code;

        private AkSkAuthException(int code, String message) {
            super(message);
            this.code = code;
        }

        private int getCode() {
            return code;
        }
    }
}
