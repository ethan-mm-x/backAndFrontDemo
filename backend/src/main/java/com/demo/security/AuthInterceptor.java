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
 * 拦截器：受保护接口必须已登录。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Set<String> WHITE_LIST = Set.of(
            "/api/health",
            "/api/auth/captcha",
            "/api/auth/login",
            "/api/auth/register"
    );

    private final ObjectMapper objectMapper;

    public AuthInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        if (WHITE_LIST.contains(path)) {
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
