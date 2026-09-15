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

    @GetMapping("/captcha")
    public ApiResult<Map<String, String>> captcha() {
        return ApiResult.ok(captchaService.create());
    }

    @PostMapping("/register")
    public ApiResult<Void> register(@Valid @RequestBody RegisterRequest req) {
        captchaService.verifyAndConsume(req.getCaptchaId(), req.getCaptchaCode());
        userService.register(req.getUsername().trim(), req.getPassword());
        return ApiResult.ok();
    }

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

    @GetMapping("/me")
    public ApiResult<LoginUser> me() {
        return ApiResult.ok(SecurityUtils.getCurrentUser());
    }
}
