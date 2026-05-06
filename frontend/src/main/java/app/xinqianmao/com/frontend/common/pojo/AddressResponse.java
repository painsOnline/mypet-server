/**
 * File: AddressResponse.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.frontend.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Delivery address response.
 */
@Data
@Schema(description = "收货地址响应")
public class AddressResponse {

    @Schema(description = "地址ID")
    private String id;

    @Schema(description = "收货人")
    private String receiver;

    @Schema(description = "联系电话")
    private String contact;

    @Schema(description = "省编码")
    private String provinceCode;

    @Schema(description = "市编码")
    private String cityCode;

    @Schema(description = "区县编码")
    private String countyCode;

    @Schema(description = "详细地址")
    private String address;

    @Schema(description = "是否默认，1=是 0=否")
    private Integer isDefault;

    @Schema(description = "省市区中文拼接")
    private String fullLocation;
}
