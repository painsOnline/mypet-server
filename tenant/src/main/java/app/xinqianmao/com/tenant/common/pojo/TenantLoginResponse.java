/**
 * File: TenantLoginResponse.java
 * Author: system
 * Date: 2026-05-21
 */
package app.xinqianmao.com.tenant.common.pojo;

import lombok.Data;

@Data
public class TenantLoginResponse {

    private String account;
    private String lastLoginTime;
    private String token;
}
