/**
 * File: PreOrderResponse.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.frontend.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Pre-order preview response (before final submission).
 */
@Data
@Schema(description = "预付单响应")
public class PreOrderResponse {

    @Schema(description = "商品列表")
    private List<ProductItem> products;

    @Schema(description = "费用汇总")
    private Summary summary;

    @Schema(description = "用户地址列表")
    private List<AddressResponse> userAddresses;

    /**
     * Product item in pre-order.
     */
    @Data
    @Schema(description = "预付单商品项")
    public static class ProductItem {

        @Schema(description = "商品ID")
        private String id;

        @Schema(description = "SKU ID")
        private String skuId;

        @Schema(description = "商品名称")
        private String name;

        @Schema(description = "规格文字")
        private String attrsText;

        @Schema(description = "数量")
        private Integer count;

        @Schema(description = "原单价")
        private BigDecimal price;

        @Schema(description = "实付单价")
        private BigDecimal payPrice;

        @Schema(description = "商品图片")
        private String picture;

        @Schema(description = "小计（原价x数量）")
        private BigDecimal totalPrice;

        @Schema(description = "实付小计（现价x数量）")
        private BigDecimal totalPayPrice;
    }

    /**
     * Order summary.
     */
    @Data
    @Schema(description = "费用汇总")
    public static class Summary {

        @Schema(description = "商品总价")
        private BigDecimal totalPrice;

        @Schema(description = "运费")
        private BigDecimal postFee;

        @Schema(description = "应付金额")
        private BigDecimal totalPayPrice;
    }
}
