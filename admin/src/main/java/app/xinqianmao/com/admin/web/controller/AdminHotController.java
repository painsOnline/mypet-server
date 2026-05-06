/**
 * File: AdminHotController.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.web.controller;

import app.xinqianmao.com.admin.common.pojo.HotProductResponse;
import app.xinqianmao.com.admin.common.pojo.HotSortRequest;
import app.xinqianmao.com.admin.service.HotProductService;
import app.xinqianmao.com.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Hot products management controller.
 */
@Tag(name = "热门推荐", description = "首页热门商品管理")
@RestController
@RequestMapping("/admin/hot")
@RequiredArgsConstructor
public class AdminHotController {

    private final HotProductService hotProductService;

    @Operation(summary = "获取热门商品列表")
    @GetMapping
    public Result<List<HotProductResponse>> listAll() {
        return Result.ok(hotProductService.listAll());
    }

    @Operation(summary = "更新热门商品排序")
    @PutMapping("/sort")
    public Result<Void> updateSort(@RequestBody HotSortRequest request) {
        hotProductService.updateSort(request);
        return Result.ok();
    }
}
