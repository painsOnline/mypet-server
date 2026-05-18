/**
 * File: CategoryController.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.frontend.web.controller;

import app.xinqianmao.com.common.annotation.NoAuth;
import app.xinqianmao.com.common.result.PageResult;
import app.xinqianmao.com.common.result.Result;
import app.xinqianmao.com.frontend.common.entity.Product;
import app.xinqianmao.com.frontend.common.entity.ProductCategory;
import app.xinqianmao.com.frontend.common.pojo.GoodsDetailResponse;
import app.xinqianmao.com.frontend.dao.ProductCategoryMapper;
import app.xinqianmao.com.frontend.dao.ProductMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "分类", description = "商品分类浏览")
@RestController
@RequestMapping("/frontend/category")
@RequiredArgsConstructor
public class CategoryController {

    private final ProductCategoryMapper categoryMapper;
    private final ProductMapper productMapper;
    private final HomeController homeController;

    @NoAuth
    @Operation(summary = "获取一级分类")
    @GetMapping("/list")
    public Result<List<ProductCategory>> listAll() {
        return Result.ok(categoryMapper.selectList(
                new LambdaQueryWrapper<ProductCategory>().eq(ProductCategory::getIsDelete, 0)
                        .orderByAsc(ProductCategory::getSort)));
    }

    @NoAuth
    @Operation(summary = "分类商品列表")
    @GetMapping("/product/list")
    public Result<PageResult<GoodsDetailResponse>> productsByCategory(
            @RequestParam String id,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "6") Integer pageSize) {
        Page<Product> p = Page.of(page, pageSize);
        IPage<Product> productPage = productMapper.selectPage(p,
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getProductCategory, id)
                        .eq(Product::getIsEnable, 1)
                        .orderByAsc(Product::getSort));
        List<GoodsDetailResponse> items = productPage.getRecords().stream()
                .map(homeController::buildGoodsDetail).collect(Collectors.toList());
        return Result.ok(PageResult.of(items, productPage.getTotal(), page, productPage.getPages(), pageSize));
    }
}
