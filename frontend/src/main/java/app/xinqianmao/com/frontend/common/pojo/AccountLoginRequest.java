/**
 * File: AccountLoginRequest.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.frontend.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Account + password login request.
 */
@Data
@Schema(description = "账号密码登录请求")
public class AccountLoginRequest {

    @NotBlank(message = "账号不能为空")
    @Schema(description = "手机号", example = "13812345678")
    private String account;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码", example = "123456")
    private String password;
}
