/**
 * File: TenantAdminSaveRequest.java
 * Author: system
 * Date: 2026-05-21
 */
package app.xinqianmao.com.tenant.common.pojo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TenantAdminSaveRequest {

    @NotBlank(message = "账号不能为空")
    private String account;

    @NotBlank(message = "密码不能为空")
    private String password;
}
