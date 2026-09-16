package com.demo.web;

import com.demo.common.ApiResult;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查：确认应用存活；有 Redis 时顺带 ping。
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    private final RedissonClient redissonClient;

    public HealthController(ObjectProvider<RedissonClient> redissonClient) {
        this.redissonClient = redissonClient.getIfAvailable();
    }

    @GetMapping("/health")
    public ApiResult<Map<String, String>> health() {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("status", "ok");
        if (redissonClient != null) {
            RBucket<String> bucket = redissonClient.getBucket("demo:health");
            bucket.set("PONG", Duration.ofSeconds(30));
            body.put("redis", bucket.get());
        } else {
            body.put("redis", "skipped");
            body.put("mode", "openapi-lite");
        }
        return ApiResult.ok(body);
    }
}
