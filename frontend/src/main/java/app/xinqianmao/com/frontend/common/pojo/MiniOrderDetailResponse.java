/**
 * File: MiniOrderDetailResponse.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.frontend.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Mini-program order detail response.
 */
@Data
@Schema(description = "小程序订单详情")
public class MiniOrderDetailResponse {

    @Schema(description = "订单ID")
    private String id;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "订单状态")
    private Integer orderState;

    @Schema(description = "订单状态描述")
    private String orderStateDesc;

    @Schema(description = "商品清单")
    private List<SkuItem> skus;

    @Schema(description = "收货人")
    private String receiverContact;

    @Schema(description = "收货电话")
    private String receiverMobile;

    @Schema(description = "收货地址")
    private String receiverAddress;

    @Schema(description = "下单时间")
    private String createTime;

    @Schema(description = "商品总价")
    private BigDecimal totalMoney;

    @Schema(description = "应付金额")
    private BigDecimal payMoney;

    @Schema(description = "实付金额")
    private BigDecimal actualPayMoney;

    @Schema(description = "配送时间")
    private String deliveryTime;

    @Schema(description = "付款时间")
    private String payTime;

    @Schema(description = "收货时间")
    private String receiveTime;

    @Schema(description = "完成时间")
    private String finishTime;

    @Schema(description = "取消时间")
    private String cancelTime;

    @Schema(description = "订单备注")
    private String buyerMessage;

    @Schema(description = "总件数")
    private Integer totalNum;

    /**
     * Order SKU detail.
     */
    @Data
    @Schema(description = "订单SKU详情")
    public static class SkuItem {

        @Schema(description = "SKU ID")
        private String id;

        @Schema(description = "商品ID")
        private String productId;

        @Schema(description = "商品名称")
        private String name;

        @Schema(description = "规格文字")
        private String attrsText;

        @Schema(description = "规格列表")
        private List<SpecItem> specs;

        @Schema(description = "购买数量")
        private Integer quantity;

        @Schema(description = "购买时单价")
        private BigDecimal price;

        @Schema(description = "购买时原价")
        private BigDecimal oldPrice;

        @Schema(description = "商品图片")
        private String picture;

        /**
         * Order SKU spec item matching WX-API format.
         */
        @Data
        @Schema(description = "订单SKU规格项")
        public static class SpecItem {

            @Schema(description = "规格名称")
            private String specName;

            @Schema(description = "规格值名称")
            private String valueName;

            @Schema(description = "规格ID")
            private String specId;

            @Schema(description = "值ID")
            private String valueId;
        }
    }
}
