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
 * Spring MVC 拦截器：保护业务接口。
 * <p>
 * 白名单（健康检查、验证码、公钥、登录、注册）以及开放 API 前缀 {@code /api/open/**} 直接放行；
 * 其它路径必须已在 Filter 里解析出登录用户，否则返回统一 JSON「未登录」。
 * <p>
 * 开放 API 的鉴权由 {@code AkSkAuthFilter}（HMAC-SM3）负责，不依赖 JWT。
 * <p>
 * 对照前端：类似 Vue Router 的 {@code beforeEach} 守卫。
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

    /** 开放 API：由 AkSkAuthFilter 做 HMAC-SM3 验签，不走 JWT */
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
