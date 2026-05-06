/**
 * File: AdminLoginRequest.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Admin login request body.
 */
@Data
@Schema(description = "管理员登录请求")
public class AdminLoginRequest {

    @NotBlank(message = "账号不能为空")
    @Schema(description = "管理员账号", example = "admin")
    private String account;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码", example = "admin123")
    private String password;
}
