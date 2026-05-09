/**
 * File: WxLoginRequest.java
 * Author: system
 * Date: 2026-05-08
 */
package app.xinqianmao.com.frontend.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * WeChat mini-program OpenID quick login request.
 */
@Data
@Schema(description = "小程序OpenID快速登录请求")
public class WxLoginRequest {

    @Schema(description = "微信 wx.login() 返回的临时凭证", example = "081x4r000x...")
    private String code;
}
