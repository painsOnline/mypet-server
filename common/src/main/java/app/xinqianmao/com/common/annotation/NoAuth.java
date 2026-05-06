/**
 * File: NoAuth.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.common.annotation;

import java.lang.annotation.*;

/**
 * Mark a controller method to skip JWT authentication.
 * Used on login endpoints.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NoAuth {
}
