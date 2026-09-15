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
 * 过滤器：解析 JWT，写入 ThreadLocal；临近过期时在响应头续期。
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

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER_AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return null;
    }
}
