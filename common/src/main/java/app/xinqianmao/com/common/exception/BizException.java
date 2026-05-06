/**
 * File: BizException.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.common.exception;

import lombok.Getter;

/**
 * Business exception with error code.
 * Use to signal known error states to the client.
 */
@Getter
public class BizException extends RuntimeException {

    private final String code;

    public BizException(String code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(String message) {
        super(message);
        this.code = "500";
    }
}
