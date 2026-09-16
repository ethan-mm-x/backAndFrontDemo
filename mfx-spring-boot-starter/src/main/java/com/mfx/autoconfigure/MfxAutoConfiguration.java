package com.mfx.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mfx.client.MfxHttpClient;
import com.mfx.service.DefaultMfxApiService;
import com.mfx.service.MfxApiService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

/**
 * MFX Starter 自动配置：注册 {@link MfxApiService}。
 */
@AutoConfiguration(after = JacksonAutoConfiguration.class)
@EnableConfigurationProperties(MfxProperties.class)
@ConditionalOnProperty(prefix = "mfx", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MfxAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MfxHttpClient mfxHttpClient(MfxProperties properties, ObjectMapper objectMapper) {
        requireConfig(properties);
        return new MfxHttpClient(properties, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public MfxApiService mfxApiService(MfxHttpClient mfxHttpClient) {
        return new DefaultMfxApiService(mfxHttpClient);
    }

    private static void requireConfig(MfxProperties properties) {
        if (!StringUtils.hasText(properties.getBaseUrl())) {
            throw new IllegalStateException("mfx.base-url 不能为空");
        }
        if (!StringUtils.hasText(properties.getAccessKey())) {
            throw new IllegalStateException("mfx.access-key 不能为空");
        }
        if (!StringUtils.hasText(properties.getSecretKey())) {
            throw new IllegalStateException("mfx.secret-key 不能为空");
        }
    }
}
