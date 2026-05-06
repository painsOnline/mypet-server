/**
 * File: AdminShopController.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.web.controller;

import app.xinqianmao.com.admin.common.entity.Shop;
import app.xinqianmao.com.admin.common.pojo.ShopSaveRequest;
import app.xinqianmao.com.admin.service.ShopService;
import app.xinqianmao.com.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Shop settings controller.
 */
@Tag(name = "店铺设置", description = "店铺基本信息和轮播图管理")
@RestController
@RequestMapping("/admin/shop")
@RequiredArgsConstructor
public class AdminShopController {

    private final ShopService shopService;

    @Operation(summary = "获取店铺配置")
    @GetMapping
    public Result<Shop> getConfig() {
        return Result.ok(shopService.getConfig());
    }

    @Operation(summary = "更新店铺配置")
    @PutMapping
    public Result<Void> updateConfig(@RequestBody ShopSaveRequest request) {
        shopService.updateConfig(request);
        return Result.ok();
    }
}
