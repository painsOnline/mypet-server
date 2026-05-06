/**
 * File: GlobalConstants.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.common.constant;

/**
 * Global constants used across modules.
 */
public final class GlobalConstants {

    private GlobalConstants() {}

    /** Tenant header name */
    public static final String TENANT_HEADER = "Tenant";

    /** Authorization header name */
    public static final String AUTHORIZATION_HEADER = "Authorization";

    /** Source client header name (for mini-program) */
    public static final String SOURCE_CLIENT_HEADER = "source-client";

    /** Bearer token prefix */
    public static final String BEARER_PREFIX = "Bearer ";

    /** Config database key in DynamicDataSource */
    public static final String CONFIG_DB_KEY = "config";

    /** Default date-time pattern (Beijing time) */
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
}
