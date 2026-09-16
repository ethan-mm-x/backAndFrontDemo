package com.demo.aksk;

import com.demo.common.ApiResult;
import com.demo.common.OperLog;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 开放 API 业务示例（故意写得很薄，方便聚焦鉴权）。
 * <p>
 * 能进到这里，说明 {@link AkSkAuthFilter} 已经验签通过。
 * <b>这里没有、也不需要</b> {@code Authorization: Bearer}。
 * <p>
 * 对照前端路由：
 * <ul>
 *   <li>{@code GET /api/open/echo} ≈ 带 query 的 GET</li>
 *   <li>{@code POST /api/open/message} ≈ axios.post JSON body</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/open")
public class OpenApiController {

    /**
     * GET 示例：query 参数会进入签名里的 canonicalQuery。
     * 改 name 的同时若不重算签名，服务端会报「签名校验失败」——可拿来做实验。
     */
    @OperLog("开放API-GET回显")
    @GetMapping("/echo")
    public ApiResult<Map<String, Object>> echo(
            @RequestParam(defaultValue = "hello") String name,
            HttpServletRequest request) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("method", "GET");
        data.put("name", name);
        // Filter 验签成功后写入的调用方名，不是 JWT 里的 username
        data.put("client", request.getAttribute(AkSkAuthFilter.ATTR_CLIENT_NAME));
        data.put("serverTime", Instant.now().toString());
        return ApiResult.ok(data);
    }

    /**
     * POST 示例：JSON body 的原始字节参与 SM3。
     * 注意：签名必须基于「真正发出去的那串字节」；多空格、字段顺序变化都可能改变 body 字节。
     */
    @OperLog("开放API-POST消息")
    @PostMapping("/message")
    public ApiResult<Map<String, Object>> message(
            @Valid @RequestBody MessageRequest body,
            HttpServletRequest request) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("method", "POST");
        data.put("title", body.getTitle());
        data.put("content", body.getContent());
        data.put("client", request.getAttribute(AkSkAuthFilter.ATTR_CLIENT_NAME));
        data.put("acceptedAt", Instant.now().toString());
        return ApiResult.ok(data);
    }

    /**
     * POST 入参 DTO。
     * {@code @Valid} + {@code @NotBlank} 对照前端：表单 rules；不通过会进全局异常变成 ApiResult。
     */
    public static class MessageRequest {
        @NotBlank(message = "title 不能为空")
        @Size(max = 64, message = "title 最长 64")
        private String title;

        @NotBlank(message = "content 不能为空")
        @Size(max = 512, message = "content 最长 512")
        private String content;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
