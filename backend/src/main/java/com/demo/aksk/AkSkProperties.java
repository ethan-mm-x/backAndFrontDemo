package com.demo.aksk;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * AK/SK 开放接口配置：客户端列表 + 时间窗。
 */
@Component
@ConfigurationProperties(prefix = "aksk")
public class AkSkProperties {

    /** 允许的时间偏差（秒），默认 5 分钟 */
    private long skewSeconds = 300;

    /** 已登记的调用方 */
    private List<Client> clients = new ArrayList<>();

    public long getSkewSeconds() {
        return skewSeconds;
    }

    public void setSkewSeconds(long skewSeconds) {
        this.skewSeconds = skewSeconds;
    }

    public List<Client> getClients() {
        return clients;
    }

    public void setClients(List<Client> clients) {
        this.clients = clients;
    }

    public static class Client {
        private String accessKey;
        private String secretKey;
        private String name;

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
