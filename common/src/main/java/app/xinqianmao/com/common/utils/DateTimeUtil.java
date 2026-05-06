/**
 * File: DateTimeUtil.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.common.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Date-time utilities for Beijing time (UTC+8).
 */
public final class DateTimeUtil {

    /** Beijing time zone */
    public static final ZoneId ZONE_BEIJING = ZoneId.of("Asia/Shanghai");

    /** Standard display format */
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateTimeUtil() {}

    /**
     * Get current Beijing time.
     */
    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE_BEIJING);
    }

    /**
     * Format LocalDateTime to string using standard pattern.
     */
    public static String format(LocalDateTime dt) {
        if (dt == null) return null;
        return dt.format(FORMATTER);
    }

    /**
     * Parse string to LocalDateTime using standard pattern.
     */
    public static LocalDateTime parse(String s) {
        if (s == null || s.isBlank()) return null;
        return LocalDateTime.parse(s, FORMATTER);
    }
}
