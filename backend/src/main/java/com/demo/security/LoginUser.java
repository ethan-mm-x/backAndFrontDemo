package com.demo.security;

/**
 * 当前登录用户快照：会放进 JWT payload，也会放进 ThreadLocal。
 * <p>
 * 不含密码等敏感字段。
 */
public class LoginUser {

    private Long userId;
    private String username;

    public LoginUser() {
    }

    public LoginUser(Long userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
