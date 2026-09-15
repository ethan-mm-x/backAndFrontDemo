package com.demo.common;

import java.io.Serializable;

/**
 * 统一 API 响应壳：{@code { code, message, data }}。
 * <p>
 * 约定：{@code code == 0} 表示成功；其它为业务/错误码（如 400、401、500）。
 * 对照前端：axios 拦截器里通常判断 {@code code !== 0} 再提示 message。
 */
public class ApiResult<T> implements Serializable {

    private int code;
    private String message;
    private T data;

    public static <T> ApiResult<T> ok(T data) {
        ApiResult<T> r = new ApiResult<>();
        r.code = 0;
        r.message = "ok";
        r.data = data;
        return r;
    }

    public static <T> ApiResult<T> ok() {
        return ok(null);
    }

    public static <T> ApiResult<T> fail(int code, String message) {
        ApiResult<T> r = new ApiResult<>();
        r.code = code;
        r.message = message;
        return r;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}
