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
 * 本机 HTTP 入口，方便 curl 验证对方 SDK。
 * 需 {@code -Pwjz-client} 且 {@code wjz.aksk.enabled=true}。
 */
@RestController
@RequestMapping("/demo")
@ConditionalOnClass(SignApiService.class)
@ConditionalOnProperty(prefix = "wjz.aksk", name = "enabled", havingValue = "true")
public class WjzDemoController {

    private final WjzRemoteCaller wjzRemoteCaller;

    public WjzDemoController(WjzRemoteCaller wjzRemoteCaller) {
        this.wjzRemoteCaller = wjzRemoteCaller;
    }

    /** 转发到对方 GET /api/demo/ping */
    @GetMapping("/ping")
    public Object ping(@RequestParam(value = "name", defaultValue = "wjz") String name) {
        try {
            return wjzRemoteCaller.ping(name);
        } catch (Exception e) {
            return ApiResult.fail(502, "调用对方 WJZ API 失败: " + e.getMessage());
        }
    }

    /** 转发到对方 POST /api/demo/echo */
    @PostMapping("/echo")
    public Object echo(@RequestParam("message") String message) {
        try {
            return wjzRemoteCaller.echo(message);
        } catch (Exception e) {
            return ApiResult.fail(502, "调用对方 WJZ API 失败: " + e.getMessage());
        }
    }
}
