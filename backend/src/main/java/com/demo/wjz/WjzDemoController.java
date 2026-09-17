package com.demo.wjz;

import com.demo.common.ApiResult;
import com.wjz.aksk.sdk.SignApiService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 「本机 HTTP 壳」：让你用普通 curl / 浏览器打本机 8080，再由后端带着 AK/SK 签名去调对方 8443。
 * <p>
 * <b>为什么要多这一层？</b>
 * <ul>
 *   <li>对方只开 HTTPS + 自签证书 + 每次要算签名头，前端/curl 直接打很烦。</li>
 *   <li>本机开一个无鉴权的 HTTP 口（{@code /demo/*}），签名细节藏在 {@link WjzRemoteCaller} 里。</li>
 *   <li>对照前端：这像 BFF（Backend For Frontend）——页面只调自家同源接口，真正的第三方签名在服务端做。</li>
 * </ul>
 * <p>
 * <b>注解速查（对标前端路由）</b>
 * <ul>
 *   <li>{@code @RestController}：这个类的方法返回值直接当 JSON 响应体（不是返回页面）。</li>
 *   <li>{@code @RequestMapping("/demo")}：类级别前缀，类似 Vue Router 的 parent path。</li>
 *   <li>{@code @GetMapping("/ping")}：完整路径 = {@code GET /demo/ping}。</li>
 *   <li>{@code @RequestParam("name")}：读 query，类似 {@code ?name=}；必须写名字，
 *       否则编译未开 {@code -parameters} 时 Spring 不知道参数叫什么。</li>
 * </ul>
 * 路径故意不放在 {@code /api/**} 下，这样不会被本项目的 JWT 登录拦截器拦住。
 * <p>
 * 启用条件与 {@link WjzRemoteCaller} 相同：classpath 有对方 SDK，且 {@code wjz.aksk.enabled=true}。
 */
@RestController
@RequestMapping("/demo")
@ConditionalOnClass(SignApiService.class)
@ConditionalOnProperty(prefix = "wjz.aksk", name = "enabled", havingValue = "true")
public class WjzDemoController {

    /**
     * 真正调对方 API 的封装；Controller 只负责「收 HTTP 参数 → 调 Service → 回写响应」。
     * 前后端分层同理：页面不直接写 axios 细节时，会再包一层 api 模块。
     */
    private final WjzRemoteCaller wjzRemoteCaller;

    public WjzDemoController(WjzRemoteCaller wjzRemoteCaller) {
        this.wjzRemoteCaller = wjzRemoteCaller;
    }

    /**
     * 本机：{@code GET http://127.0.0.1:8080/demo/ping?name=wjz}
     * → 后端签名后请求对方：{@code GET https://host:8443/api/demo/ping?name=wjz}
     * <p>
     * 成功时透传对方 JSON（一般含 {@code code/message/data}）；
     * 失败时返回本项目的 {@link ApiResult}，避免只看到笼统的「服务器内部错误」。
     */
    @GetMapping("/ping")
    public Object ping(@RequestParam(value = "name", defaultValue = "wjz") String name) {
        try {
            return wjzRemoteCaller.ping(name);
        } catch (Exception e) {
            // 502：上游（对方 API）不通或签名/网络失败，不是本机业务校验错误
            return ApiResult.fail(502, "调用对方 WJZ API 失败: " + e.getMessage());
        }
    }

    /**
     * 本机：{@code POST http://127.0.0.1:8080/demo/echo?message=hi}
     * → 后端签名后请求对方：{@code POST /api/demo/echo}，body {@code {"message":"hi"}}。
     * <p>
     * Demo 用 query 传 message 只是为了 curl 方便；真正业务里 POST 更常见用 JSON body。
     * 对方接口本身吃的是 JSON body，转换发生在 {@link WjzRemoteCaller#echo(String)}。
     */
    @PostMapping("/echo")
    public Object echo(@RequestParam("message") String message) {
        try {
            return wjzRemoteCaller.echo(message);
        } catch (Exception e) {
            return ApiResult.fail(502, "调用对方 WJZ API 失败: " + e.getMessage());
        }
    }
}
