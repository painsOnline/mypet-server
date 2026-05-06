/**
 * File: TenantContext.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.common.auth;

/**
 * ThreadLocal holder for current request tenant code.
 * Set by TenantInterceptor, used by DynamicDataSource for routing.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(String tenantCode) {
        CONTEXT.set(tenantCode);
    }

    public static String get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
