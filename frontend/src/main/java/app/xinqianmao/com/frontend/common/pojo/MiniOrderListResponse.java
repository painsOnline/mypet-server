/**
 * File: MiniOrderListResponse.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.frontend.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Mini-program order list item response.
 */
@Data
@Schema(description = "小程序订单列表项")
public class MiniOrderListResponse {

    @Schema(description = "订单ID")
    private String id;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "订单状态：1=待配送 2=配送中 3=已收货 4=已完成 5=已取消")
    private Integer orderState;

    @Schema(description = "商品清单")
    private List<SkuItem> skus;

    @Schema(description = "收货人")
    private String receiverContact;

    @Schema(description = "收货电话")
    private String receiverMobile;

    @Schema(description = "收货地址")
    private String receiverAddress;

    @Schema(description = "下单时间 yyyy-MM-dd HH:mm:ss")
    private String createTime;

    @Schema(description = "商品总价（原价x数量之和）")
    private BigDecimal totalMoney;

    @Schema(description = "应付金额（现价x数量之和）")
    private BigDecimal payMoney;

    @Schema(description = "实付金额")
    private BigDecimal actualPayMoney;

    @Schema(description = "总件数")
    private Integer totalNum;

    /**
     * Order SKU item in list.
     */
    @Data
    @Schema(description = "订单SKU项")
    public static class SkuItem {

        @Schema(description = "SKU ID")
        private String id;

        @Schema(description = "商品ID")
        private String productId;

        @Schema(description = "商品名称")
        private String name;

        @Schema(description = "规格文字")
        private String attrsText;

        @Schema(description = "购买数量")
        private Integer quantity;

        @Schema(description = "购买时单价")
        private BigDecimal price;

        @Schema(description = "购买时原价")
        private BigDecimal oldPrice;

        @Schema(description = "商品图片")
        private String picture;
    }
}
