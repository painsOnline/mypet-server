/**
 * File: MemberLoginResponse.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.frontend.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Member login response containing user info and JWT token.
 */
@Data
@Schema(description = "会员登录响应")
public class MemberLoginResponse {

    @Schema(description = "用户ID")
    private String id;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "账号")
    private String account;

    @Schema(description = "手机号")
    private String mobile;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "JWT登录凭证")
    private String token;
}
