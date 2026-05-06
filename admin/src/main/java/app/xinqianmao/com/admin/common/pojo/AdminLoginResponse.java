/**
 * File: AdminLoginResponse.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Admin login response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理员登录响应")
public class AdminLoginResponse {

    @Schema(description = "管理员账号")
    private String account;

    @Schema(description = "最后登录时间")
    private String lastLoginTime;

    @Schema(description = "JWT令牌")
    private String token;
}
