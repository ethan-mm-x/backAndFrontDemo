package com.demo.aksk;

import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis Nonce（有 Redisson 时启用）。
 */
@Component
@ConditionalOnBean(RedissonClient.class)
public class RedisNonceStore implements NonceStore {

    private final RedissonClient redissonClient;

    public RedisNonceStore(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public boolean tryAcquire(String key, Duration ttl) {
        RBucket<String> bucket = redissonClient.getBucket(key);
        return bucket.setIfAbsent("1", ttl);
    }
}
