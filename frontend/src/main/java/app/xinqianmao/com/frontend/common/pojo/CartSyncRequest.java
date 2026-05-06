/**
 * File: CartSyncRequest.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.frontend.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Cart sync item for PUT /member/cart request body.
 * Same fields as CartItemResponse.
 */
@Data
@Schema(description = "购物车同步请求项")
public class CartSyncRequest {

    @Schema(description = "商品ID")
    private String id;

    @Schema(description = "SKU ID")
    private String skuId;

    @Schema(description = "商品名称")
    private String name;

    @Schema(description = "图片")
    private String picture;

    @Schema(description = "数量")
    private Integer count;

    @Schema(description = "加入时原价")
    private BigDecimal price;

    @Schema(description = "加入时现价")
    private BigDecimal nowPrice;

    @Schema(description = "当前库存")
    private Integer stock;

    @Schema(description = "是否选中")
    private Boolean selected;

    @Schema(description = "规格文字")
    private String attrsText;

    @Schema(description = "是否有效（下架则false）")
    private Boolean isEffective;
}
