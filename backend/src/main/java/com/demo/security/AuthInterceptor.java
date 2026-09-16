package com.demo.security;

import com.demo.common.ApiResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Spring MVC 拦截器：保护「需要登录用户」的业务接口。
 * <p>
 * 白名单（健康检查、验证码、公钥、登录、注册）以及开放 API 前缀 {@code /api/open/**} 直接放行；
 * 其它路径必须已在 {@code JwtAuthFilter} 里解析出登录用户，否则返回统一 JSON「未登录」。
 * <p>
 * <b>为何开放 API 也要放行？</b>
 * 开放接口走的是 {@code AkSkAuthFilter}（HMAC-SM3），没有 JWT，ThreadLocal 里通常没有登录用户。
 * 若不放行，这里会误报「未登录」，把已经验过签的请求挡掉。
 * <p>
 * 对照前端：类似 Vue Router 的 {@code beforeEach}；开放路由相当于 meta 里标了 {@code public: true}。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Set<String> WHITE_LIST = Set.of(
            "/api/health",
            "/api/auth/captcha",
            "/api/auth/public-key",
            "/api/auth/login",
            "/api/auth/register"
    );

    /** 开放 API 前缀：鉴权交给 AkSkAuthFilter，这里不再要 JWT */
    private static final String OPEN_API_PREFIX = "/api/open/";

    private final ObjectMapper objectMapper;

    public AuthInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 预检请求不拦
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        // 登录白名单 或 开放 API → 不要求 JWT
        if (WHITE_LIST.contains(path) || path.startsWith(OPEN_API_PREFIX)) {
            return true;
        }
        if (SecurityUtils.getCurrentUserOrNull() != null) {
            return true;
        }
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResult.fail(401, "未登录或登录已失效")));
        return false;
    }
}
