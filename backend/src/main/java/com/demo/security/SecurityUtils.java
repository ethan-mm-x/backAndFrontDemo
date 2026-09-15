package com.demo.security;

import com.demo.common.BizException;

/**
 * 静态方法获取当前登录用户（依赖 Filter 写入的 ThreadLocal）。
 */
public final class SecurityUtils {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private SecurityUtils() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser getCurrentUser() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw new BizException(401, "当前未登录");
        }
        return user;
    }

    public static LoginUser getCurrentUserOrNull() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
