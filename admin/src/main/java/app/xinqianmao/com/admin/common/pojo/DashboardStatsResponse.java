/**
 * File: DashboardStatsResponse.java
 * Author: system
 * Date: 2026-05-04
 */
package app.xinqianmao.com.admin.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "仪表盘统计数据")
public class DashboardStatsResponse {

    @Schema(description = "商品总数")
    private long productCount;

    @Schema(description = "订单总数")
    private long orderCount;

    @Schema(description = "待处理订单数")
    private long pendingOrderCount;

    @Schema(description = "用户总数")
    private long userCount;
}
