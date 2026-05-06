/**
 * File: OrderStatusEnum.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.common.enums;

import lombok.Getter;

/**
 * Order status state machine:
 * 1=PENDING_DELIVERY -> 2=IN_DELIVERY -> 3=RECEIVED -> 4=COMPLETED
 * 1,3 -> 5=CANCELLED
 */
@Getter
public enum OrderStatusEnum {

    PENDING_DELIVERY(1, "待配送"),
    IN_DELIVERY(2, "配送中"),
    RECEIVED(3, "已收货"),
    COMPLETED(4, "已完成"),
    CANCELLED(5, "已取消");

    private final int code;
    private final String desc;

    OrderStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static OrderStatusEnum fromCode(int code) {
        for (OrderStatusEnum e : values()) {
            if (e.code == code) return e;
        }
        return null;
    }
}
