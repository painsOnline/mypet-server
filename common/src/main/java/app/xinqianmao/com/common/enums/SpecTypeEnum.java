/**
 * File: SpecTypeEnum.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.common.enums;

import lombok.Getter;

/**
 * Spec type: 1=SKU related (affects price/stock), 2=Display only parameter.
 */
@Getter
public enum SpecTypeEnum {

    SKU(1, "SKU规格"),
    DISPLAY(2, "展示参数");

    private final int code;
    private final String desc;

    SpecTypeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
