package com.demo.auth;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.util.IdUtil;
import com.demo.common.BizException;
import com.demo.config.CaptchaProperties;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class CaptchaService {

    private static final String KEY_PREFIX = "captcha:";

    private final RedissonClient redissonClient;
    private final CaptchaProperties properties;

    public CaptchaService(RedissonClient redissonClient, CaptchaProperties properties) {
        this.redissonClient = redissonClient;
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
        RBucket<String> bucket = redissonClient.getBucket(KEY_PREFIX + captchaId, StringCodec.INSTANCE);
        bucket.set(captcha.getCode().toLowerCase(), Duration.ofSeconds(properties.getTtlSeconds()));

        Map<String, String> body = new LinkedHashMap<>();
        body.put("captchaId", captchaId);
        body.put("imageBase64", captcha.getImageBase64Data());
        return body;
    }

    public void verifyAndConsume(String captchaId, String captchaCode) {
        if (captchaId == null || captchaCode == null) {
            throw new BizException("验证码不能为空");
        }
        RBucket<String> bucket = redissonClient.getBucket(KEY_PREFIX + captchaId, StringCodec.INSTANCE);
        String cached = bucket.getAndDelete();
        if (cached == null) {
            throw new BizException("验证码已失效，请刷新");
        }
        if (!cached.equalsIgnoreCase(captchaCode.trim())) {
            throw new BizException("验证码错误");
        }
    }
}
