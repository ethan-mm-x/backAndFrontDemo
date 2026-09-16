package com.demo.aksk;

import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 进程内 Nonce（无 Redis 时使用，如 openapi 单机测试）。
 */
@Component
@ConditionalOnMissingBean(RedissonClient.class)
public class MemoryNonceStore implements NonceStore {

    private final ConcurrentHashMap<String, Long> store = new ConcurrentHashMap<>();

    @Override
    public boolean tryAcquire(String key, Duration ttl) {
        long expireAt = System.currentTimeMillis() + ttl.toMillis();
        purgeExpired();
        Long prev = store.putIfAbsent(key, expireAt);
        return prev == null;
    }

    private void purgeExpired() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Long>> it = store.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> e = it.next();
            if (e.getValue() < now) {
                it.remove();
            }
        }
    }
}
