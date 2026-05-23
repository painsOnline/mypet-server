/**
 * File: ShopSaveRequest.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Shop config save request.
 */
@Data
@Schema(description = "店铺设置请求")
public class ShopSaveRequest {

    @Schema(description = "店铺名称")
    private String name;

    @Schema(description = "店铺Logo URL")
    private String logo;

    @Schema(description = "免配送费门槛")
    private BigDecimal freeShippingAmount;

    @Schema(description = "轮播图JSON")
    private String banners;

    @Schema(description = "店铺详情（富文本）")
    private String detail;
}
