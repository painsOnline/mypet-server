/**
 * File: OrderTrendResponse.java
 * Author: system
 * Date: 2026-05-05
 */
package app.xinqianmao.com.admin.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "订单趋势统计")
public class OrderTrendResponse {

    private List<TrendPoint> points;

    @Data
    @Schema(description = "趋势数据点")
    public static class TrendPoint {
        @Schema(description = "日期键（日:YYYY-MM-DD, 月:YYYY-MM）")
        private String dateKey;
        @Schema(description = "订单数量")
        private Long orderCount;
        @Schema(description = "订单实付总金额")
        private java.math.BigDecimal totalAmount;
    }
}
