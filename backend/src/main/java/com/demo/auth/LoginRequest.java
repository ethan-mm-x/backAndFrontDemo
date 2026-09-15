package com.demo.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求体。字段上的注解由 Controller 的 {@code @Valid} 触发校验。
 */
public class LoginRequest {

    /** 用户名 */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 明文密码（仅传输，落库前会 BCrypt） */
    @NotBlank(message = "密码不能为空")
    private String password;

    /** 获取验证码接口返回的 id，对应 Redis key */
    @NotBlank(message = "验证码ID不能为空")
    private String captchaId;

    /** 用户看到的验证码字符 */
    @NotBlank(message = "验证码不能为空")
    private String captchaCode;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCaptchaId() {
        return captchaId;
    }

    public void setCaptchaId(String captchaId) {
        this.captchaId = captchaId;
    }

    public String getCaptchaCode() {
        return captchaCode;
    }

    public void setCaptchaCode(String captchaCode) {
        this.captchaCode = captchaCode;
    }
}
