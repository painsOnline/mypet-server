/**
 * File: AdminOrderController.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.web.controller;

import app.xinqianmao.com.admin.common.pojo.OrderDetailResponse;
import app.xinqianmao.com.admin.common.pojo.OrderListResponse;
import app.xinqianmao.com.admin.common.pojo.OrderSearchRequest;
import app.xinqianmao.com.admin.service.OrderService;
import app.xinqianmao.com.common.result.PageResult;
import app.xinqianmao.com.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Order management controller.
 */
@Tag(name = "订单管理", description = "订单查询、发货、收货、退款处理、取消")
@RestController
@RequestMapping("/admin/order")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @Operation(summary = "待处理订单", description = "待配送和申请退货退款的订单")
    @GetMapping("/pending")
    public Result<List<OrderListResponse>> pending() {
        return Result.ok(orderService.getPendingOrders());
    }

    @Operation(summary = "订单搜索列表")
    @GetMapping
    public Result<PageResult<OrderListResponse>> search(OrderSearchRequest request) {
        return Result.ok(PageResult.of(orderService.search(request)));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{id}")
    public Result<OrderDetailResponse> detail(@PathVariable String id) {
        return Result.ok(orderService.getDetail(id));
    }

    @Operation(summary = "发货", description = "订单状态 待配送→配送中")
    @PutMapping("/{id}/dispatch")
    public Result<Void> dispatch(@PathVariable String id) {
        orderService.dispatch(id);
        return Result.ok();
    }

    @Operation(summary = "确认收货", description = "订单状态 配送中→已收货")
    @PutMapping("/{id}/confirmReceipt")
    public Result<Void> confirmReceipt(@PathVariable String id) {
        orderService.confirmReceipt(id);
        return Result.ok();
    }

    @Operation(summary = "同意退货退款", description = "订单状态 已收货→已取消")
    @PutMapping("/{id}/approveRefund")
    public Result<Void> approveRefund(@PathVariable String id) {
        orderService.approveRefund(id);
        return Result.ok();
    }

    @Operation(summary = "拒绝退货退款", description = "拒绝退货申请")
    @PutMapping("/{id}/rejectRefund")
    public Result<Void> rejectRefund(@PathVariable String id) {
        orderService.rejectRefund(id);
        return Result.ok();
    }

    @Operation(summary = "取消订单")
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable String id) {
        orderService.cancel(id);
        return Result.ok();
    }
}
