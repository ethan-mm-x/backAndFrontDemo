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

/**
 * 图形验证码：用 Hutool 生成图片，答案用 Redisson 存 Redis（带 TTL），校验后删除（一次性）。
 * <p>
 * 必须用 {@link StringCodec}，避免默认编解码把字符串存成带控制字符的二进制，导致前端回传后 JSON 解析失败。
 */
@Service
public class CaptchaService {

    private static final String KEY_PREFIX = "captcha:";

    private final RedissonClient redissonClient;
    private final CaptchaProperties properties;

    public CaptchaService(RedissonClient redissonClient, CaptchaProperties properties) {
        this.redissonClient = redissonClient;
        this.properties = properties;
    }

    /**
     * 生成验证码。
     *
     * @return captchaId（给前端下次提交）、imageBase64（直接当 img src）
     */
    public Map<String, String> create() {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(
                properties.getWidth(),
                properties.getHeight(),
                properties.getCodeCount(),
                20
        );
        String captchaId = IdUtil.fastSimpleUUID();
        RBucket<String> bucket = redissonClient.getBucket(KEY_PREFIX + captchaId, StringCodec.INSTANCE);
        // 小写存，校验时忽略大小写
        bucket.set(captcha.getCode().toLowerCase(), Duration.ofSeconds(properties.getTtlSeconds()));

        Map<String, String> body = new LinkedHashMap<>();
        body.put("captchaId", captchaId);
        body.put("imageBase64", captcha.getImageBase64Data());
        return body;
    }

    /**
     * 校验验证码并立刻删除（防止重复使用）。
     */
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
