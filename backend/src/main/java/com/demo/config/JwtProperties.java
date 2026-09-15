package com.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** SM2 私钥 hex */
    private String privateKeyHex;

    /** SM2 公钥 hex */
    private String publicKeyHex;

    /** Token 有效期（毫秒） */
    private long expireMs = 7_200_000L;

    /** 剩余有效期低于该阈值时自动续期（毫秒） */
    private long renewThresholdMs = 1_800_000L;

    public String getPrivateKeyHex() {
        return privateKeyHex;
    }

    public void setPrivateKeyHex(String privateKeyHex) {
        this.privateKeyHex = privateKeyHex;
    }

    public String getPublicKeyHex() {
        return publicKeyHex;
    }

    public void setPublicKeyHex(String publicKeyHex) {
        this.publicKeyHex = publicKeyHex;
    }

    public long getExpireMs() {
        return expireMs;
    }

    public void setExpireMs(long expireMs) {
        this.expireMs = expireMs;
    }

    public long getRenewThresholdMs() {
        return renewThresholdMs;
    }

    public void setRenewThresholdMs(long renewThresholdMs) {
        this.renewThresholdMs = renewThresholdMs;
    }
}
