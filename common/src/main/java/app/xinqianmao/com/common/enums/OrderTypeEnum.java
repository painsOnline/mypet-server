/**
 * File: OrderTypeEnum.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.common.enums;

import lombok.Getter;

/**
 * Order type: 0=Prepaid (default for cash-on-delivery), 1=Confirmed.
 */
@Getter
public enum OrderTypeEnum {

    PREPAID(0, "预付"),
    CONFIRMED(1, "确认");

    private final int code;
    private final String desc;

    OrderTypeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
