/**
 * File: AdminBrandController.java
 * Author: system
 * Date: 2026-05-11
 */
package app.xinqianmao.com.admin.web.controller;

import app.xinqianmao.com.admin.common.entity.ProductBrand;
import app.xinqianmao.com.admin.service.BrandService;
import app.xinqianmao.com.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "品牌管理", description = "商品品牌增删改查")
@RestController
@RequestMapping("/admin/brand")
@RequiredArgsConstructor
public class AdminBrandController {

    private final BrandService brandService;

    @Operation(summary = "品牌列表")
    @GetMapping
    public Result<List<ProductBrand>> list() {
        return Result.ok(brandService.listAll());
    }

    @Operation(summary = "新增品牌")
    @PostMapping
    public Result<ProductBrand> create(@RequestBody ProductBrand brand) {
        return Result.ok(brandService.create(brand));
    }

    @Operation(summary = "修改品牌")
    @PutMapping("/{id}")
    public Result<ProductBrand> update(@PathVariable String id, @RequestBody ProductBrand brand) {
        brand.setId(id);
        return Result.ok(brandService.update(brand));
    }

    @Operation(summary = "删除品牌（软删除）")
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable String id) {
        brandService.delete(id);
        return Result.ok(id);
    }
}
