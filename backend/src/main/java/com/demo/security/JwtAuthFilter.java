package com.demo.security;

import com.demo.common.BizException;
import com.demo.jwt.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet 过滤器（在 Interceptor / Controller 之前执行）。
 * <p>
 * 职责：
 * <ul>
 *   <li>从 {@code Authorization: Bearer xxx} 取出 JWT</li>
 *   <li>验签成功则把用户写入 {@link SecurityUtils}（ThreadLocal）</li>
 *   <li>临近过期时在响应头写 {@code X-New-Token}，前端可自动替换本地 token</li>
 *   <li>{@code finally} 里必须 {@code clear()}，避免线程池复用导致用户串号</li>
 * </ul>
 * 有 token 但无效时这里吞掉异常，交给 {@link AuthInterceptor} 决定是否必须登录。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_NEW_TOKEN = "X-New-Token";

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String token = resolveToken(request);
            if (StringUtils.hasText(token)) {
                try {
                    LoginUser user = jwtUtil.parseToken(token);
                    SecurityUtils.set(user);
                    if (jwtUtil.shouldRenew(token)) {
                        String renewed = jwtUtil.createToken(user);
                        response.setHeader(HEADER_NEW_TOKEN, renewed);
                        // 让浏览器/axios 能读到自定义响应头
                        response.setHeader("Access-Control-Expose-Headers", HEADER_NEW_TOKEN);
                    }
                } catch (BizException ignored) {
                    // 交给拦截器判断是否必须登录
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            SecurityUtils.clear();
        }
    }

    /** 解析 Bearer Token；没有或格式不对返回 null。 */
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER_AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return null;
    }
}
