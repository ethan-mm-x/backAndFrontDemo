package com.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 绑定 {@code application.yml} 里 {@code captcha.*}：图片尺寸、位数、Redis TTL。
 */
@Component
@ConfigurationProperties(prefix = "captcha")
public class CaptchaProperties {

    /** Redis 中验证码存活秒数 */
    private long ttlSeconds = 300;
    private int width = 130;
    private int height = 48;
    /** 验证码字符个数 */
    private int codeCount = 4;

    public long getTtlSeconds() {
        return ttlSeconds;
    }

    public void setTtlSeconds(long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getCodeCount() {
        return codeCount;
    }

    public void setCodeCount(int codeCount) {
        this.codeCount = codeCount;
    }
}
