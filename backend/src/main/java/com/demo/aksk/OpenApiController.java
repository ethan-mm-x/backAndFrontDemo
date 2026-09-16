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
 * 开放 API 示例：走 AK/SK（HMAC-SM3）鉴权，不走 JWT。
 * <ul>
 *   <li>GET {@code /api/open/echo}</li>
 *   <li>POST {@code /api/open/message}</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/open")
public class OpenApiController {

    @OperLog("开放API-GET回显")
    @GetMapping("/echo")
    public ApiResult<Map<String, Object>> echo(
            @RequestParam(defaultValue = "hello") String name,
            HttpServletRequest request) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("method", "GET");
        data.put("name", name);
        data.put("client", request.getAttribute(AkSkAuthFilter.ATTR_CLIENT_NAME));
        data.put("serverTime", Instant.now().toString());
        return ApiResult.ok(data);
    }

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
