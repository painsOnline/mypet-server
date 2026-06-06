/**
 * File: AdminProductController.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.web.controller;

import app.xinqianmao.com.admin.common.pojo.*;
import app.xinqianmao.com.admin.service.ProductService;
import app.xinqianmao.com.admin.service.ProductSkuService;
import app.xinqianmao.com.common.result.PageResult;
import app.xinqianmao.com.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Product management controller.
 */
@Tag(name = "商品管理", description = "商品的增删改查、上下架、热门推荐")
@RestController
@RequestMapping("/admin/product")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;
    private final ProductSkuService skuService;

    @Operation(summary = "商品搜索列表")
    @GetMapping
    public Result<PageResult<ProductListResponse>> search(ProductSearchRequest request) {
        return Result.ok(PageResult.of(productService.search(request)));
    }

    @Operation(summary = "商品详情")
    @GetMapping("/{id}")
    public Result<ProductDetailResponse> detail(@PathVariable String id) {
        return Result.ok(productService.getDetail(id));
    }

    @Operation(summary = "新增商品")
    @PostMapping
    public Result<String> create(@Valid @RequestBody ProductSaveRequest request) {
        return Result.ok(productService.create(request));
    }

    @Operation(summary = "编辑商品")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id, @Valid @RequestBody ProductSaveRequest request) {
        productService.update(id, request);
        return Result.ok();
    }

    @Operation(summary = "删除商品")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        productService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "切换热门推荐")
    @PutMapping("/{id}/hot")
    public Result<Void> toggleHot(@PathVariable String id) {
        productService.toggleHot(id);
        return Result.ok();
    }

    @Operation(summary = "切换上架/下架")
    @PutMapping("/{id}/enable")
    public Result<Void> toggleEnable(@PathVariable String id) {
        productService.toggleEnable(id);
        return Result.ok();
    }

    @Operation(summary = "预览SKU组合", description = "根据商品类型生成所有可能的SKU规格组合")
    @GetMapping("/generateSkus/{productTypeId}")
    public Result<List<Map<String, String>>> generateSkus(@PathVariable String productTypeId) {
        return Result.ok(skuService.previewCombinations(productTypeId));
    }

    @Operation(summary = "批量更新商品排序")
    @PutMapping("/sort")
    public Result<Void> updateSort(@RequestBody ProductSortRequest request) {
        productService.updateSort(request);
        return Result.ok();
    }
}
