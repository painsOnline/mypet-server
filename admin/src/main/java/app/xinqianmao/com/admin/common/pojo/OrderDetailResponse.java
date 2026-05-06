/**
 * File: OrderDetailResponse.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Order detail response.
 */
@Data
@Schema(description = "订单详情")
public class OrderDetailResponse {

    private String id;
    private Integer orderStatus;
    private String orderStatusDesc;
    private BigDecimal totalMoney;
    private BigDecimal payMoney;
    private BigDecimal actualPayMoney;
    private String createTime;
    private String modifyTime;

    private MemberInfo member;
    private ReceiverInfo receiver;
    private List<ProductItem> products;

    @Data
    @Schema(description = "用户信息")
    public static class MemberInfo {
        private String id;
        private String mobile;
        private String nickname;
        private String avatar;
    }

    @Data
    @Schema(description = "收货人信息")
    public static class ReceiverInfo {
        private String id;
        private String receiver;
        private String contact;
        private String address;
        private String fullLocation;
    }

    @Data
    @Schema(description = "订单商品")
    public static class ProductItem {
        private String productId;
        private String name;
        private String picture;
        private BigDecimal price;
        private BigDecimal oldPrice;
        private List<SkuDetail> skus;
    }

    @Data
    @Schema(description = "订单SKU")
    public static class SkuDetail {
        private String skuId;
        private BigDecimal price;
        private BigDecimal oldPrice;
        private Integer quantity;
        private String picture;
        private String attrsText;
    }
}
