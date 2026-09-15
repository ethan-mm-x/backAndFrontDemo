package com.demo.security;

import com.demo.common.BizException;

/**
 * 用静态方法取「当前请求的登录用户」。
 * <p>
 * 数据来自 {@link JwtAuthFilter} 写入的 {@link ThreadLocal}。
 * 业务代码里直接 {@code SecurityUtils.getCurrentUser()} 即可，不必层层传参。
 * <p>
 * 注意：只能在处理 HTTP 请求的线程里用；请求结束后 Filter 会 {@link #clear()}。
 */
public final class SecurityUtils {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private SecurityUtils() {
    }

    /** Filter 解析 JWT 成功后调用。 */
    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    /** 必须已登录；否则抛 401 业务异常。 */
    public static LoginUser getCurrentUser() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw new BizException(401, "当前未登录");
        }
        return user;
    }

    /** 可空：拦截器白名单判断、AOP 打日志时用。 */
    public static LoginUser getCurrentUserOrNull() {
        return HOLDER.get();
    }

    /** 请求结束清理，防止线程池复用串用户。 */
    public static void clear() {
        HOLDER.remove();
    }
}
