package com.demo.aksk;

import java.time.Duration;

/**
 * Nonce 防重放存储。生产用 Redis；openapi 测试模式用内存。
 */
public interface NonceStore {

    /**
     * @return true 表示首次使用；false 表示已存在（疑似重放）
     */
    boolean tryAcquire(String key, Duration ttl);
}
