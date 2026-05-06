/**
 * File: SubmitRequest.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.frontend.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * Submit order request.
 */
@Data
@Schema(description = "提交订单请求")
public class SubmitRequest {

    @NotBlank(message = "收货地址不能为空")
    @Schema(description = "收货地址ID", example = "1")
    private String addressId;

    @Schema(description = "配送时间：1=不限 2=工作日 3=周末", example = "1")
    private Integer deliveryTimeType;

    @Schema(description = "订单备注")
    private String buyerMessage;

    @NotEmpty(message = "商品列表不能为空")
    @Schema(description = "商品列表")
    private List<ProductItem> products;

    @Schema(description = "支付渠道，固定传1", example = "1")
    private Integer payChannel;

    @Schema(description = "支付方式，固定传1", example = "1")
    private Integer payType;

    /**
     * Product item in order submission.
     */
    @Data
    @Schema(description = "订单商品项")
    public static class ProductItem {

        @Schema(description = "SKU ID")
        private String skuId;

        @Schema(description = "数量")
        private Integer count;
    }
}
