package com.demo.aksk;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 绑定 {@code application.yml} 里的 {@code aksk.*} 配置。
 * <p>
 * 对照前端：类似把 {@code .env} / {@code vite.config} 里的变量读进一个配置对象。
 * Spring 的 {@code @ConfigurationProperties(prefix = "aksk")} 会把 YAML 字段自动灌进下面的属性。
 * <p>
 * 学习时请对照打开 {@code application.yml} 底部的 {@code aksk:} 段。
 */
@Component
@ConfigurationProperties(prefix = "aksk")
public class AkSkProperties {

    /**
     * 允许的客户端/服务端时钟偏差（秒）。
     * 超出则判「请求过期」——防止把很久以前截获的请求拿来重放。
     */
    private long skewSeconds = 300;

    /** 已登记的调用方列表：每个调用方一对 AK/SK */
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

    /**
     * 一个开放平台客户。
     * <ul>
     *   <li>{@code accessKey}：可出现在请求头里（身份 ID）</li>
     *   <li>{@code secretKey}：只存在服务端配置与调用方保密存储，绝不走网络</li>
     *   <li>{@code name}：人类可读名字，验签通过后塞进 request attribute 给业务用</li>
     * </ul>
     */
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
