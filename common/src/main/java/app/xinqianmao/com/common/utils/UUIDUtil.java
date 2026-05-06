/**
 * File: UUIDUtil.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.common.utils;

import java.util.UUID;

/**
 * UUID generation utility.
 */
public final class UUIDUtil {

    private UUIDUtil() {}

    /**
     * Generate UUID string with dashes (standard 36-char format).
     */
    public static String uuid() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generate UUID string without dashes (32-char format).
     */
    public static String uuidSimple() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
