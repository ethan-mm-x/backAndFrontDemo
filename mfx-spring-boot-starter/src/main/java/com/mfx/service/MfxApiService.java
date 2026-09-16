package com.mfx.service;

import java.util.Map;

/**
 * 使用方注入的 API 服务：自动 AK/SK（HMAC-SM3）签名后发起 HTTP 请求。
 */
public interface MfxApiService {

    /**
     * GET，返回响应原文。
     *
     * @param path  相对路径，如 {@code /api/open/echo}
     * @param query 查询参数（可为 null）
     */
    String get(String path, Map<String, ?> query);

    /**
     * GET，反序列化为指定类型。
     */
    <T> T get(String path, Map<String, ?> query, Class<T> responseType);

    /**
     * POST JSON 字符串 body。
     */
    String post(String path, String jsonBody);

    /**
     * POST：对象序列化为 JSON（与签名使用同一份字节）。
     */
    <T> T post(String path, Object body, Class<T> responseType);
}
