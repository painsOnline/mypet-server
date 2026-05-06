/**
 * File: AdminCategoryController.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.web.controller;

import app.xinqianmao.com.admin.common.entity.ProductCategory;
import app.xinqianmao.com.admin.common.pojo.CategoryListResponse;
import app.xinqianmao.com.admin.common.pojo.CategorySaveRequest;
import app.xinqianmao.com.admin.service.CategoryService;
import app.xinqianmao.com.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Product category management controller.
 */
@Tag(name = "分类管理", description = "商品分类的增删改查")
@RestController
@RequestMapping("/admin/category")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "获取全部分类")
    @GetMapping
    public Result<List<CategoryListResponse>> listAll() {
        return Result.ok(categoryService.listAll());
    }

    @Operation(summary = "新增分类")
    @PostMapping
    public Result<ProductCategory> create(@Valid @RequestBody CategorySaveRequest request) {
        return Result.ok(categoryService.create(request));
    }

    @Operation(summary = "编辑分类")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id, @Valid @RequestBody CategorySaveRequest request) {
        categoryService.update(id, request);
        return Result.ok();
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        categoryService.delete(id);
        return Result.ok();
    }
}
