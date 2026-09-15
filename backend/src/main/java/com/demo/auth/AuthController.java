package com.demo.auth;

import com.demo.common.ApiResult;
import com.demo.security.LoginUser;
import com.demo.security.SecurityUtils;
import com.demo.jwt.JwtUtil;
import com.demo.user.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 认证相关 HTTP 接口：验证码、注册、登录、当前用户。
 * <p>
 * 对照前端：相当于 {@code api/auth.ts} 里请求的后端实现。
 * {@code @Valid} 会触发入参 DTO 上的校验注解（如 {@code @NotBlank}）。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final CaptchaService captchaService;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(CaptchaService captchaService, UserService userService, JwtUtil jwtUtil) {
        this.captchaService = captchaService;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    /** 获取图形验证码：返回 captchaId + imageBase64，答案存在 Redis。 */
    @GetMapping("/captcha")
    public ApiResult<Map<String, String>> captcha() {
        return ApiResult.ok(captchaService.create());
    }

    /** 注册：先校验并消费验证码，再 BCrypt 加密密码写入 sys_user。 */
    @PostMapping("/register")
    public ApiResult<Void> register(@Valid @RequestBody RegisterRequest req) {
        captchaService.verifyAndConsume(req.getCaptchaId(), req.getCaptchaCode());
        userService.register(req.getUsername().trim(), req.getPassword());
        return ApiResult.ok();
    }

    /** 登录：校验验证码与账号密码，签发 SM2 JWT。 */
    @PostMapping("/login")
    public ApiResult<Map<String, Object>> login(@Valid @RequestBody LoginRequest req) {
        captchaService.verifyAndConsume(req.getCaptchaId(), req.getCaptchaCode());
        LoginUser user = userService.login(req.getUsername().trim(), req.getPassword());
        String token = jwtUtil.createToken(user);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", token);
        data.put("userId", user.getUserId());
        data.put("username", user.getUsername());
        return ApiResult.ok(data);
    }

    /** 当前登录用户（需带 Token；从 ThreadLocal 读取）。 */
    @GetMapping("/me")
    public ApiResult<LoginUser> me() {
        return ApiResult.ok(SecurityUtils.getCurrentUser());
    }
}
