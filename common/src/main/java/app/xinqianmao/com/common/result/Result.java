/**
 * File: Result.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * Unified API response wrapper.
 * All controller methods must return this type.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> {

    private String code;
    private String msg;
    private T result;

    private Result() {}

    private Result(String code, String msg, T result) {
        this.code = code;
        this.msg = msg;
        this.result = result;
    }

    public static Result<Void> ok() {
        return new Result<>("200", "success", null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>("200", "success", data);
    }

    public static <T> Result<T> ok(String msg, T data) {
        return new Result<>("200", msg, data);
    }

    public static <T> Result<T> error(String code, String msg) {
        return new Result<>(code, msg, null);
    }

    public static <T> Result<T> error(String msg) {
        return new Result<>("500", msg, null);
    }
}
