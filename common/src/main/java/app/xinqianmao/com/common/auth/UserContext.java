/**
 * File: UserContext.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.common.auth;

/**
 * ThreadLocal holder for current request user info.
 * Set by AuthInterceptor, accessible in any downstream code.
 */
public final class UserContext {

    private static final ThreadLocal<UserAuthInfo> CONTEXT = new ThreadLocal<>();

    private UserContext() {}

    public static void set(UserAuthInfo info) {
        CONTEXT.set(info);
    }

    public static UserAuthInfo get() {
        return CONTEXT.get();
    }

    public static String getUserId() {
        UserAuthInfo info = get();
        return info != null ? info.getUserId() : null;
    }

    /**
     * Get userId or throw — for endpoints that require login.
     */
    public static String getRequiredUserId() {
        String userId = getUserId();
        if (userId == null) {
            throw new RuntimeException("未登录");
        }
        return userId;
    }

    public static String getTenantCode() {
        UserAuthInfo info = get();
        return info != null ? info.getTenantCode() : null;
    }

    public static boolean isAdmin() {
        UserAuthInfo info = get();
        return info != null && info.isAdmin();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
