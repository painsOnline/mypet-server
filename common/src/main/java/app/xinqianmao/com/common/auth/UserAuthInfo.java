/**
 * File: UserAuthInfo.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.common.auth;

import lombok.Data;

/**
 * POJO holding authenticated user information extracted from JWT.
 */
@Data
public class UserAuthInfo {

    private String userId;
    private String tenantCode;
    private boolean isAdmin;
}
