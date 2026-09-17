package com.demo.wjz;

import com.wjz.aksk.sdk.SignApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 「调用方」业务层：用对方发来的 Spring Boot SDK 调对方的 HTTPS API。
 * <p>
 * <b>和前端直觉的对照</b>
 * <ul>
 *   <li>{@link SignApiService} ≈ 已经配好「请求拦截器」的 axios 实例：
 *       每次请求自动加 {@code X-Access-Key / X-Timestamp / X-Nonce / X-Signature}，
 *       你不用手写 HMAC-SM3。</li>
 *   <li>{@code get/post(path, ...)} 里的 path 只写路径（如 {@code /api/demo/ping}），
 *       完整 host 在 {@code application.yml} 的 {@code wjz.aksk.base-url}，
 *       类似 axios 的 {@code baseURL}。</li>
 *   <li>本类 ≈ 前端的 {@code api/xxx.ts}：封装具体接口，Controller 再调它。</li>
 * </ul>
 * <p>
 * <b>为什么有两个条件注解？</b>
 * <ul>
 *   <li>{@code @ConditionalOnClass(SignApiService.class)}：classpath 里没有对方 jar 时，
 *       这个 Bean 根本不注册 → Docker 镜像构建可以不拉公司 Nexus。</li>
 *   <li>{@code @ConditionalOnProperty(... enabled=true)}：配置开关关掉时也不注册，
 *       避免日常启动去连对方 8443。</li>
 * </ul>
 * 本地启用：{@code ./mvnw -Pwjz-client spring-boot:run -Dspring-boot.run.profiles=openapi,wjz}
 * （或仓库脚本 {@code ./scripts/wjz-client-demo.sh}）
 * <p>
 * 实现了 {@link ApplicationRunner}：Spring Boot 启动完成后自动跑一次 {@link #run}，
 * 相当于「服务起来后立刻自测 ping/echo」，方便看控制台有没有 {@code code=0}。
 */
@Component
@ConditionalOnClass(SignApiService.class)
@ConditionalOnProperty(prefix = "wjz.aksk", name = "enabled", havingValue = "true")
public class WjzRemoteCaller implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(WjzRemoteCaller.class);

    /**
     * 对方 SDK 提供的客户端。
     * <p>
     * {@code final} + 构造器注入 = Spring 启动时把唯一的 {@link SignApiService} Bean 塞进来。
     * 前端类比：不能写 {@code new Axios()}，而是用已经配好拦截器的共享实例。
     */
    private final SignApiService signApiService;

    public WjzRemoteCaller(SignApiService signApiService) {
        this.signApiService = signApiService;
    }

    /**
     * 调对方 {@code GET /api/demo/ping?name=...}。
     * <p>
     * 第三个参数 {@code Object.class}：把响应 JSON 反序列化成通用结构（通常是 Map）。
     * 生产里可以换成具体 DTO 类，类似前端的 {@code axios.get&lt;PingResp&gt;(...)}。
     *
     * @param name query 参数，对应对方文档里的可选 {@code name}
     */
    public Object ping(String name) {
        // Map.of 生成不可变 Map，当 query；SDK 会按 key 排序后参与签名
        return signApiService.get("/api/demo/ping", Map.of("name", name), Object.class);
    }

    /**
     * 调对方 {@code POST /api/demo/echo}，body 为 JSON {@code {"message":"..."}}。
     * <p>
     * 注意：参与签名的是「发出去的原始 JSON 字节」，所以不要自己 pretty-print；
     * SDK 会按统一方式序列化 body，避免签名对不上。
     *
     * @param message 对方要求的必填字段
     */
    public Object echo(String message) {
        return signApiService.post("/api/demo/echo", Map.of("message", message), Object.class);
    }

    /**
     * 启动后自动联调一次。失败只打日志，不让整个 Spring 启动失败
     * （对方服务挂了时，我们自己的 backend 仍应能起来）。
     */
    @Override
    public void run(ApplicationArguments args) {
        try {
            Object ping = ping("backend-demo");
            log.info("WJZ ping => {}", ping);
            System.out.println("WJZ ping => " + ping);

            Object echo = echo("hello-from-backend");
            log.info("WJZ echo => {}", echo);
            System.out.println("WJZ echo => " + echo);
        } catch (Exception e) {
            log.warn("WJZ 调用失败（对方 https://172.16.22.152:8443 可能未就绪）: {}", e.toString());
            System.out.println("WJZ 调用失败: " + e.getMessage());
        }
    }
}
