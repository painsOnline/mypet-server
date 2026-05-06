/**
 * File: OrderSearchRequest.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Order search request.
 */
@Data
@Schema(description = "订单搜索请求")
public class OrderSearchRequest {

    @Schema(description = "最低价格")
    private BigDecimal priceMin;

    @Schema(description = "最高价格")
    private BigDecimal priceMax;

    @Schema(description = "下单开始时间")
    private String createTimeStart;

    @Schema(description = "下单结束时间")
    private String createTimeEnd;

    @Schema(description = "用户手机号")
    private String userPhone;

    @Schema(description = "订单状态: 1-待配送,2-配送中,3-已收货,4-已完成,5-已取消")
    private Integer orderStatus;

    @Schema(description = "排序字段")
    private String sortBy;

    @Schema(description = "排序方向")
    private String sortOrder;

    @Schema(description = "页码", example = "1")
    private Long page = 1L;

    @Schema(description = "每页条数", example = "10")
    private Long pageSize = 10L;
}
