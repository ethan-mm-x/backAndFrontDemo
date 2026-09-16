package com.demo.wjz;

import com.wjz.aksk.sdk.SignApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 直接注入对方 SDK，调用 {@link SignApiService}。
 * 启动后会打一次 ping / echo；对方 8443 不可达时只打日志，不阻断本服务启动。
 */
@Component
public class WjzRemoteCaller implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(WjzRemoteCaller.class);

    private final SignApiService signApiService;

    public WjzRemoteCaller(SignApiService signApiService) {
        this.signApiService = signApiService;
    }

    /** GET /api/demo/ping */
    public Object ping(String name) {
        return signApiService.get("/api/demo/ping", Map.of("name", name), Object.class);
    }

    /** POST /api/demo/echo */
    public Object echo(String message) {
        return signApiService.post("/api/demo/echo", Map.of("message", message), Object.class);
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            Object ping = ping("backend-demo");
            log.info("WJZ ping => {}", ping);
            System.out.println("WJZ ping => " + ping);

            Object echo = echo("hello-from-backend");
            log.info("WJZ echo => {}", echo);
            System.out.println("WJZ echo => " + echo);
        } catch (Exception e) {
            log.warn("WJZ 调用失败（对方 https://172.16.22.148:8443 可能未就绪）: {}", e.toString());
            System.out.println("WJZ 调用失败: " + e.getMessage());
        }
    }
}
