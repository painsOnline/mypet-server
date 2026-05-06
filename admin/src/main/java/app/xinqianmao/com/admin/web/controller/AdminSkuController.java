/**
 * File: AdminSkuController.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.web.controller;

import app.xinqianmao.com.admin.common.entity.ProductSku;
import app.xinqianmao.com.admin.common.pojo.SkuSaveRequest;
import app.xinqianmao.com.admin.service.ProductSkuService;
import app.xinqianmao.com.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SKU management controller.
 */
@Tag(name = "SKU管理", description = "商品SKU的增删改查和自动生成")
@RestController
@RequestMapping("/admin/product/{productId}/sku")
@RequiredArgsConstructor
public class AdminSkuController {

    private final ProductSkuService skuService;

    @Operation(summary = "获取商品SKU列表")
    @GetMapping
    public Result<List<ProductSku>> list(@PathVariable String productId) {
        return Result.ok(skuService.listByProduct(productId));
    }

    @Operation(summary = "新增SKU")
    @PostMapping
    public Result<Void> add(@PathVariable String productId, @RequestBody SkuSaveRequest request) {
        skuService.add(productId, null, request);
        return Result.ok();
    }

    @Operation(summary = "编辑SKU")
    @PutMapping("/{skuId}")
    public Result<Void> update(@PathVariable String skuId, @RequestBody SkuSaveRequest request) {
        skuService.update(skuId, request);
        return Result.ok();
    }

    @Operation(summary = "删除SKU")
    @DeleteMapping("/{skuId}")
    public Result<Void> delete(@PathVariable String skuId) {
        skuService.delete(skuId);
        return Result.ok();
    }

    @Operation(summary = "自动生成SKU组合", description = "根据商品类型的SKU规格定义，自动生成所有规格组合的SKU")
    @PostMapping("/generate")
    public Result<Void> generate(@PathVariable String productId, @RequestParam String typeId) {
        skuService.generate(productId, typeId);
        return Result.ok();
    }
}
