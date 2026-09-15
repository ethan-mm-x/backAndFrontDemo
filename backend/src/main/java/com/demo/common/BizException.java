package com.demo.common;

/**
 * 业务异常：Service / 工具类主动抛出，由 {@link GlobalExceptionHandler} 转成 ApiResult。
 * <p>
 * 默认 code=400；登录相关可用 401。
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        this(400, message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
