/**
 * File: ShopSaveRequest.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

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

    @Schema(description = "轮播图URL列表")
    private List<String> banners;
}
