package com.mfx.service;

import com.mfx.client.MfxHttpClient;

import java.util.Map;

/**
 * {@link MfxApiService} 默认实现，委托 {@link MfxHttpClient}。
 */
public class DefaultMfxApiService implements MfxApiService {

    private final MfxHttpClient httpClient;

    public DefaultMfxApiService(MfxHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public String get(String path, Map<String, ?> query) {
        return httpClient.get(path, query);
    }

    @Override
    public <T> T get(String path, Map<String, ?> query, Class<T> responseType) {
        return httpClient.get(path, query, responseType);
    }

    @Override
    public String post(String path, String jsonBody) {
        return httpClient.post(path, jsonBody);
    }

    @Override
    public <T> T post(String path, Object body, Class<T> responseType) {
        return httpClient.post(path, body, responseType);
    }
}
