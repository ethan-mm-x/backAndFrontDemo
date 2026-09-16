package com.demo.auth;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.util.IdUtil;
import com.demo.common.BizException;
import com.demo.config.CaptchaProperties;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图形验证码：有 Redis 用 Redisson；openapi 轻量模式退化为进程内存（仅单机测试）。
 */
@Service
public class CaptchaService {

    private static final String KEY_PREFIX = "captcha:";

    private final RedissonClient redissonClient;
    private final CaptchaProperties properties;
    private final ConcurrentHashMap<String, LongExpire> memoryStore = new ConcurrentHashMap<>();

    public CaptchaService(ObjectProvider<RedissonClient> redissonClient, CaptchaProperties properties) {
        this.redissonClient = redissonClient.getIfAvailable();
        this.properties = properties;
    }

    public Map<String, String> create() {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(
                properties.getWidth(),
                properties.getHeight(),
                properties.getCodeCount(),
                20
        );
        String captchaId = IdUtil.fastSimpleUUID();
        String code = captcha.getCode().toLowerCase();
        Duration ttl = Duration.ofSeconds(properties.getTtlSeconds());
        if (redissonClient != null) {
            RBucket<String> bucket = redissonClient.getBucket(KEY_PREFIX + captchaId, StringCodec.INSTANCE);
            bucket.set(code, ttl);
        } else {
            memoryStore.put(captchaId, new LongExpire(code, System.currentTimeMillis() + ttl.toMillis()));
        }

        Map<String, String> body = new LinkedHashMap<>();
        body.put("captchaId", captchaId);
        body.put("imageBase64", captcha.getImageBase64Data());
        return body;
    }

    public void verifyAndConsume(String captchaId, String captchaCode) {
        if (captchaId == null || captchaCode == null) {
            throw new BizException("验证码不能为空");
        }
        String cached;
        if (redissonClient != null) {
            RBucket<String> bucket = redissonClient.getBucket(KEY_PREFIX + captchaId, StringCodec.INSTANCE);
            cached = bucket.getAndDelete();
        } else {
            LongExpire entry = memoryStore.remove(captchaId);
            cached = (entry == null || entry.expireAt < System.currentTimeMillis()) ? null : entry.code;
        }
        if (cached == null) {
            throw new BizException("验证码已失效，请刷新");
        }
        if (!cached.equalsIgnoreCase(captchaCode.trim())) {
            throw new BizException("验证码错误");
        }
    }

    private record LongExpire(String code, long expireAt) {
    }
}
