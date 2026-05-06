/**
 * File: AdminSpecsController.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.web.controller;

import app.xinqianmao.com.admin.common.pojo.SpecValuesUpdateRequest;
import app.xinqianmao.com.admin.common.pojo.SpecsSaveRequest;
import app.xinqianmao.com.admin.service.SpecsService;
import app.xinqianmao.com.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Specs management controller (nested under product type).
 */
@Tag(name = "规格管理", description = "商品类型下规格的增删改查")
@RestController
@RequestMapping("/admin/spec")
@RequiredArgsConstructor
public class AdminSpecsController {

    private final SpecsService specsService;

    @Operation(summary = "新增规格")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody SpecsSaveRequest request) {
        specsService.add(request.getProductType(), request);
        return Result.ok();
    }

    @Operation(summary = "编辑规格")
    @PutMapping("/{specId}")
    public Result<Void> update(@PathVariable String specId, @Valid @RequestBody SpecsSaveRequest request) {
        specsService.update(specId, request);
        return Result.ok();
    }

    @Operation(summary = "更新规格值列表")
    @PutMapping("/{specId}/values")
    public Result<Void> updateValues(@PathVariable String specId, @RequestBody SpecValuesUpdateRequest request) {
        specsService.updateValues(specId, request.getInputOptions());
        return Result.ok();
    }

    @Operation(summary = "删除规格")
    @DeleteMapping("/{specId}")
    public Result<Void> delete(@PathVariable String specId) {
        specsService.delete(specId);
        return Result.ok();
    }
}
