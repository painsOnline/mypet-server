/**
 * File: AdminShopLlmConfigController.java
 * Author: system
 * Date: 2026-06-14
 */
package app.xinqianmao.com.admin.web.controller;

import app.xinqianmao.com.admin.common.entity.ShopLlmConfig;
import app.xinqianmao.com.admin.common.pojo.ShopLlmConfigSaveRequest;
import app.xinqianmao.com.admin.service.ShopLlmConfigService;
import app.xinqianmao.com.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI应用配置", description = "大模型参数配置")
@RestController
@RequestMapping("/admin/shop/llm-config")
@RequiredArgsConstructor
public class AdminShopLlmConfigController {

    private final ShopLlmConfigService service;

    @Operation(summary = "获取大模型配置")
    @GetMapping
    public Result<ShopLlmConfig> getConfig() {
        return Result.ok(service.getConfig());
    }

    @Operation(summary = "保存大模型配置")
    @PutMapping
    public Result<Void> saveConfig(@RequestBody ShopLlmConfigSaveRequest request) {
        service.saveConfig(request);
        return Result.ok();
    }
}
