/**
 * File: DashboardController.java
 * Author: system
 * Date: 2026-05-04
 */
package app.xinqianmao.com.admin.web.controller;

import app.xinqianmao.com.admin.common.pojo.DashboardStatsResponse;
import app.xinqianmao.com.admin.service.DashboardService;
import app.xinqianmao.com.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "仪表盘", description = "首页统计数据")
@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "获取仪表盘统计数据")
    @GetMapping("/stats")
    public Result<DashboardStatsResponse> stats() {
        return Result.ok(dashboardService.getStats());
    }
}
