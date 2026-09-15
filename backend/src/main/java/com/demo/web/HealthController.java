package com.demo.web;

import com.demo.common.ApiResult;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查：确认应用存活，并写一条 Redis 证明 Redisson 连通。
 * <p>
 * 路径在登录白名单内，无需 Token。
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    private final RedissonClient redissonClient;

    public HealthController(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @GetMapping("/health")
    public ApiResult<Map<String, String>> health() {
        RBucket<String> bucket = redissonClient.getBucket("demo:health");
        bucket.set("PONG", Duration.ofSeconds(30));
        Map<String, String> body = new LinkedHashMap<>();
        body.put("status", "ok");
        body.put("redis", bucket.get());
        return ApiResult.ok(body);
    }
}
