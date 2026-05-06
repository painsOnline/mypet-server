/**
 * File: WxLoginRequest.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.frontend.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * WeChat mini-program login request body.
 */
@Data
@Schema(description = "小程序微信登录请求")
public class WxLoginRequest {

    @Schema(description = "微信 wx.login() 返回的临时凭证", example = "081x4r000x...")
    private String code;

    @Schema(description = "微信 getPhoneNumber 返回的加密数据")
    private String encryptedData;

    @Schema(description = "加密算法的初始向量")
    private String iv;
}
