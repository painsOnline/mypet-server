/**
 * File: AddressSaveRequest.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.frontend.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Create/update delivery address request.
 */
@Data
@Schema(description = "保存收货地址请求")
public class AddressSaveRequest {

    @NotBlank(message = "收货人不能为空")
    @Schema(description = "收货人姓名", example = "曹某人")
    private String receiver;

    @NotBlank(message = "联系电话不能为空")
    @Schema(description = "手机号", example = "15921769899")
    private String contact;

    @NotBlank(message = "省编码不能为空")
    @Schema(description = "省编码", example = "440000")
    private String provinceCode;

    @NotBlank(message = "市编码不能为空")
    @Schema(description = "市编码", example = "441300")
    private String cityCode;

    @NotBlank(message = "区编码不能为空")
    @Schema(description = "区县编码", example = "惠阳区")
    private String countyCode;

    @NotBlank(message = "详细地址不能为空")
    @Schema(description = "详细地址", example = "星河丹堤花园F区2栋3023")
    private String address;

    @Schema(description = "是否默认，1=是 0=否")
    private Integer isDefault;
}
