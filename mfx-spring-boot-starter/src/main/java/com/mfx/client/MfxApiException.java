package com.mfx.client;

/**
 * MFX API 调用失败（HTTP 非 2xx 或序列化/网络错误）。
 */
public class MfxApiException extends RuntimeException {

    private final int status;
    private final String responseBody;

    public MfxApiException(String message) {
        this(message, -1, null, null);
    }

    public MfxApiException(String message, Throwable cause) {
        this(message, -1, null, cause);
    }

    public MfxApiException(String message, int status, String responseBody) {
        this(message, status, responseBody, null);
    }

    public MfxApiException(String message, int status, String responseBody, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.responseBody = responseBody;
    }

    public int getStatus() {
        return status;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
