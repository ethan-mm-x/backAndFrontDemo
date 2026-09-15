package com.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码编码器：提供 BCrypt {@link PasswordEncoder} Bean。
 * <p>
 * 注册时 {@code encode}，登录时 {@code matches}。本 Demo 未引入完整 Spring Security 过滤器链，
 * 只复用其密码算法库。
 */
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
