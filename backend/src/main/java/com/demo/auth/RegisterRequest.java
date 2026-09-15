package com.demo.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 注册请求体。password 为 SM2 密文 hex，明文长度在解密后校验。
 */
public class RegisterRequest {

    /** 用户名，3～32 位 */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 32, message = "用户名长度需在 3-32 之间")
    private String username;

    /** SM2 公钥加密后的密码密文（hex） */
    @NotBlank(message = "密码不能为空")
    @Size(max = 1024, message = "密码密文过长")
    private String password;

    /** 验证码 id */
    @NotBlank(message = "验证码ID不能为空")
    private String captchaId;

    /** 验证码内容 */
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
