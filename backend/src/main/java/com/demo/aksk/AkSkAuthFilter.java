package com.demo.aksk;

import com.demo.common.ApiResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
 * 开放 API 的「门卫」：只拦 {@code /api/open/**}，做 AK/SK + 国密 HMAC-SM3 验签。
 * <p>
 * 对照前端：
 * <ul>
 *   <li>类似 axios <b>响应前</b> 的全局拦截，但跑在服务端、对所有调用方生效</li>
 *   <li>和 {@code JwtAuthFilter} 是两条链：这里不看 Bearer Token</li>
 * </ul>
 * 请求头约定：
 * <ul>
 *   <li>{@code X-Access-Key} — AK</li>
 *   <li>{@code X-Timestamp} — Unix 秒</li>
 *   <li>{@code X-Nonce} — 随机串（同一 AK 下不可复用）</li>
 *   <li>{@code X-Signature} — HMAC-SM3 hex</li>
 * </ul>
 * 验签成功后把调用方名称放到 request attribute，业务 Controller 可以读。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10) // 比 JwtAuthFilter 更靠前一点
public class AkSkAuthFilter extends OncePerRequestFilter {

    public static final String HEADER_ACCESS_KEY = "X-Access-Key";
    public static final String HEADER_TIMESTAMP = "X-Timestamp";
    public static final String HEADER_NONCE = "X-Nonce";
    public static final String HEADER_SIGNATURE = "X-Signature";
    /** Controller 用 request.getAttribute(ATTR_CLIENT_NAME) 取「是谁调用的」 */
    public static final String ATTR_CLIENT_NAME = "aksk.clientName";

    private final AkSkProperties properties;
    private final NonceStore nonceStore;
    private final ObjectMapper objectMapper;
    /** AK → 客户端配置，避免每次线性扫列表 */
    private final Map<String, AkSkProperties.Client> clientIndex = new ConcurrentHashMap<>();

    public AkSkAuthFilter(AkSkProperties properties, NonceStore nonceStore, ObjectMapper objectMapper) {
        this.properties = properties;
        this.nonceStore = nonceStore;
        this.objectMapper = objectMapper;
        rebuildIndex();
    }

    private void rebuildIndex() {
        clientIndex.clear();
        clientIndex.putAll(properties.getClients().stream()
                .filter(c -> StringUtils.hasText(c.getAccessKey()))
                .collect(Collectors.toMap(AkSkProperties.Client::getAccessKey, Function.identity(), (a, b) -> a)));
    }

    /** 返回 true = 本 Filter 不管。只关心开放 API 前缀。 */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/api/open/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 浏览器跨域预检不带业务头，直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // 先缓存 body，再验签，再把「可重复读」的 request 交给后面的链
        CachedBodyHttpServletRequest wrapped = new CachedBodyHttpServletRequest(request);
        try {
            verify(wrapped);
            filterChain.doFilter(wrapped, response);
        } catch (AkSkAuthException ex) {
            // 验签失败：不进 Controller，直接统一 JSON（和全局异常风格一致）
            writeFail(response, ex.getCode(), ex.getMessage());
        }
    }

    /**
     * 验签四步曲（学习时按这个顺序跟断点）：
     * <ol>
     *   <li>头齐不齐、AK 是否登记</li>
     *   <li>时间戳是否在 skew 窗口内</li>
     *   <li>Nonce 是否第一次出现（Redis setIfAbsent）</li>
     *   <li>用 SK 重算签名并比对</li>
     * </ol>
     */
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

        // 防重放：同一个 AK + Nonce 在时间窗内只能成功一次
        String nonceKey = "aksk:nonce:" + accessKey + ":" + nonce;
        boolean firstUse = nonceStore.tryAcquire(nonceKey, Duration.ofSeconds(properties.getSkewSeconds()));
        if (!firstUse) {
            throw new AkSkAuthException(401, "Nonce 已使用，疑似重放");
        }

        // 与客户端 AkSkClientDemo 使用同一套 AkSkSigner
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

        // 给后面的 Controller 用：相当于「当前开放客户」上下文（不是登录用户）
        request.setAttribute(ATTR_CLIENT_NAME,
                StringUtils.hasText(client.getName()) ? client.getName() : accessKey);
    }

    private void writeFail(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResult.fail(code, message)));
    }

    /** Filter 内部失败用，避免和业务 BizException 搅在一起。 */
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
