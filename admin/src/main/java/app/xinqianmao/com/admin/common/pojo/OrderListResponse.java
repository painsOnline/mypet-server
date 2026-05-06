/**
 * File: OrderListResponse.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Order list item response.
 */
@Data
@Schema(description = "订单列表项")
public class OrderListResponse {

    private String id;
    private Integer orderStatus;
    private String orderStatusDesc;
    private BigDecimal totalMoney;
    private BigDecimal payMoney;
    private BigDecimal actualPayMoney;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private List<SkuItem> skus;
    private Integer totalNum;
    private String createTime;
    private String modifyTime;
    private String dispatchTime;
    private String receiptTime;
    private String cancelTime;
    private String memberAvatar;
    private String memberMobile;

    @Data
    @Schema(description = "订单SKU简略信息")
    public static class SkuItem {
        private String skuId;
        private String productId;
        private String name;
        private String attrsText;
        private Integer quantity;
        private BigDecimal price;
        private String picture;
    }
}
