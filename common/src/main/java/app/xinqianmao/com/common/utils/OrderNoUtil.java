/**
 * File: OrderNoUtil.java
 * Author: system
 * Date: 2026-05-11
 *
 * Generates unique order numbers for the pet supplies store.
 * Format: "ORD" + yyyyMMdd + 6 random digits.
 */
package app.xinqianmao.com.common.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public final class OrderNoUtil {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private OrderNoUtil() {}

    /**
     * Generate a unique order number.
     * Format: ORD + yyyyMMdd + 6 random digits (e.g., ORD20260511123456).
     *
     * @return the generated order number string
     */
    public static String generate() {
        String date = LocalDate.now().format(DATE_FMT);
        int random = ThreadLocalRandom.current().nextInt(1_000_000);
        return "ORD" + date + String.format("%06d", random);
    }
}
