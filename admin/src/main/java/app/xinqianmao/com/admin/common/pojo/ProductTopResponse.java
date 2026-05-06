/**
 * File: ProductTopResponse.java
 * Author: system
 * Date: 2026-05-05
 */
package app.xinqianmao.com.admin.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "商品销量排行")
public class ProductTopResponse {

    private List<TopItem> items;

    @Data
    @Schema(description = "销量排行项")
    public static class TopItem {
        @Schema(description = "商品ID")
        private String productId;
        @Schema(description = "商品名称")
        private String productName;
        @Schema(description = "销售数量")
        private Long totalSales;
    }
}
