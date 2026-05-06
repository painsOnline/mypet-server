/**
 * File: SpecInputTypeEnum.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.common.enums;

import lombok.Getter;

/**
 * Spec input type: 1=Unique (single value), 2=Single select, 3=Multi select.
 */
@Getter
public enum SpecInputTypeEnum {

    UNIQUE(1, "唯一"),
    SINGLE(2, "单选"),
    MULTI(3, "多选");

    private final int code;
    private final String desc;

    SpecInputTypeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
