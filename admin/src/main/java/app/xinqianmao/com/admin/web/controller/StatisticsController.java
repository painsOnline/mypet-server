/**
 * File: StatisticsController.java
 * Author: system
 * Date: 2026-05-05
 */
package app.xinqianmao.com.admin.web.controller;

import app.xinqianmao.com.admin.common.pojo.OrderTrendResponse;
import app.xinqianmao.com.admin.common.pojo.ProductTopResponse;
import app.xinqianmao.com.admin.service.StatisticsService;
import app.xinqianmao.com.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "数据统计", description = "销售额统计和商品统计")
@RestController
@RequestMapping("/admin/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @Operation(summary = "订单趋势（数量和金额）")
    @GetMapping("/order-trend")
    public Result<OrderTrendResponse> orderTrend(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.ok(statisticsService.getOrderTrend(startDate, endDate));
    }

    @Operation(summary = "商品销量TOP20")
    @GetMapping("/product-top")
    public Result<ProductTopResponse> productTop(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.ok(statisticsService.getProductTop(startDate, endDate));
    }
}
