package com.mfx.sign;

/**
 * 与开放 API 服务端约定一致的 AK/SK 请求头。
 */
public final class AkSkHeaders {

    public static final String ACCESS_KEY = "X-Access-Key";
    public static final String TIMESTAMP = "X-Timestamp";
    public static final String NONCE = "X-Nonce";
    public static final String SIGNATURE = "X-Signature";

    private AkSkHeaders() {
    }
}
