/**
 * File: LoginRequest.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.frontend.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Simple phone-number login request (used by MemberLoginService.loginByPhone).
 */
@Data
@Schema(description = "手机号登录请求")
public class LoginRequest {

    @NotBlank(message = "手机号不能为空")
    @Schema(description = "手机号码", example = "13812345678")
    private String phoneNumber;
}
