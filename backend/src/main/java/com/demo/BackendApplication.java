package com.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 启动入口。
 * <p>
 * {@code @SpringBootApplication} 会扫描本包及子包，自动装配 Web、数据源、Redis 等。
 * 对照前端：类似 {@code npm run dev} 启动 Vite，这里是启动嵌入式 Tomcat。
 */
@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
