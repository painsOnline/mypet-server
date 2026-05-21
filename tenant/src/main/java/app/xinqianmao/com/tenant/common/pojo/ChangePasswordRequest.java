/**
 * File: ChangePasswordRequest.java
 * Author: system
 * Date: 2026-05-21
 */
package app.xinqianmao.com.tenant.common.pojo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}
