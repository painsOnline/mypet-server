/**
 * File: AdminProductTypeController.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.web.controller;

import app.xinqianmao.com.admin.common.entity.ProductType;
import app.xinqianmao.com.admin.common.entity.ProductSpecs;
import app.xinqianmao.com.admin.common.pojo.TypeSaveRequest;
import app.xinqianmao.com.admin.common.pojo.TypeWithSpecsResponse;
import app.xinqianmao.com.admin.service.ProductTypeService;
import app.xinqianmao.com.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "商品类型管理", description = "商品类型的增删改查")
@RestController
@RequestMapping("/admin/type")
@RequiredArgsConstructor
public class AdminProductTypeController {

    private final ProductTypeService typeService;

    @Operation(summary = "获取全部商品类型（含规格列表）")
    @GetMapping
    public Result<List<TypeWithSpecsResponse>> listAll() {
        return Result.ok(typeService.listAllWithSpecs());
    }

    @Operation(summary = "新增商品类型")
    @PostMapping
    public Result<ProductType> create(@Valid @RequestBody TypeSaveRequest request) {
        return Result.ok(typeService.create(request));
    }

    @Operation(summary = "编辑商品类型")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id, @Valid @RequestBody TypeSaveRequest request) {
        typeService.update(id, request);
        return Result.ok();
    }

    @Operation(summary = "删除商品类型")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        typeService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "获取类型的规格列表")
    @GetMapping("/{typeId}/specs")
    public Result<List<ProductSpecs>> getSpecs(@PathVariable String typeId) {
        return Result.ok(typeService.getSpecs(typeId));
    }
}
