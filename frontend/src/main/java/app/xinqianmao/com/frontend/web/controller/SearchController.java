/**
 * File: SearchController.java
 * Author: system
 * Date: 2026-05-07
 */
package app.xinqianmao.com.frontend.web.controller;

import app.xinqianmao.com.common.annotation.NoAuth;
import app.xinqianmao.com.common.result.PageResult;
import app.xinqianmao.com.common.result.Result;
import app.xinqianmao.com.frontend.common.entity.Product;
import app.xinqianmao.com.frontend.common.entity.ProductProperty;
import app.xinqianmao.com.frontend.common.entity.ProductSku;
import app.xinqianmao.com.frontend.common.entity.ProductSpecs;
import app.xinqianmao.com.frontend.common.pojo.GoodsDetailResponse;
import app.xinqianmao.com.frontend.dao.ProductMapper;
import app.xinqianmao.com.frontend.dao.ProductPropertyMapper;
import app.xinqianmao.com.frontend.dao.ProductSkuMapper;
import app.xinqianmao.com.frontend.dao.ProductSpecsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "搜索", description = "商品全文搜索")
@RestController
@RequestMapping("/frontend/search")
@RequiredArgsConstructor
public class SearchController {

    private final ProductMapper productMapper;
    private final ProductPropertyMapper propertyMapper;
    private final ProductSkuMapper skuMapper;
    private final ProductSpecsMapper specsMapper;
    private final HomeController homeController;

    @NoAuth
    @Operation(summary = "商品搜索")
    @GetMapping
    public Result<PageResult<GoodsDetailResponse>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        if (keyword == null || keyword.isBlank()) {
            return Result.ok(PageResult.of(List.of(), 0, page, 0, pageSize));
        }

        String kw = keyword.trim();
        Set<String> productIds = new LinkedHashSet<>();

        // 1. Match by product name
        productMapper.selectList(new LambdaQueryWrapper<Product>()
                .like(Product::getName, kw)
                .eq(Product::getIsEnable, 1))
                .forEach(p -> productIds.add(p.getId()));

        // 2. Match by product desc
        productMapper.selectList(new LambdaQueryWrapper<Product>()
                .like(Product::getDesc, kw)
                .eq(Product::getIsEnable, 1))
                .forEach(p -> productIds.add(p.getId()));

        // 3. Match by product detail (HTML content)
        productMapper.selectList(new LambdaQueryWrapper<Product>()
                .like(Product::getDetail, kw)
                .eq(Product::getIsEnable, 1))
                .forEach(p -> productIds.add(p.getId()));

        // 4. Match by property value (direct)
        List<ProductProperty> matchedProps = propertyMapper.selectList(
                new LambdaQueryWrapper<ProductProperty>()
                        .eq(ProductProperty::getIsDelete, 0)
                        .like(ProductProperty::getValueName, kw));
        for (ProductProperty pp : matchedProps) {
            productIds.add(pp.getProductId());
        }

        // 5. Match by spec name (via specsId lookup)
        List<ProductSpecs> matchedSpecs = specsMapper.selectList(
                new LambdaQueryWrapper<ProductSpecs>().like(ProductSpecs::getName, kw));
        if (!matchedSpecs.isEmpty()) {
            List<String> specsIds = matchedSpecs.stream().map(ProductSpecs::getId).collect(Collectors.toList());
            List<ProductProperty> propsBySpec = propertyMapper.selectList(
                    new LambdaQueryWrapper<ProductProperty>()
                            .eq(ProductProperty::getIsDelete, 0)
                            .in(ProductProperty::getSpecsId, specsIds));
            for (ProductProperty pp : propsBySpec) {
                productIds.add(pp.getProductId());
            }
        }

        // 6. Match by SKU specs (JSON text)
        List<ProductSku> allSkus = skuMapper.selectList(new LambdaQueryWrapper<>());
        for (ProductSku sku : allSkus) {
            if (sku.getSpecs() != null && sku.getSpecs().contains(kw)) {
                productIds.add(sku.getProductId());
            }
        }

        if (productIds.isEmpty()) {
            return Result.ok(PageResult.of(List.of(), 0, page, 0, pageSize));
        }

        // Fetch and sort matching products by sort order
        List<Product> products = productMapper.selectBatchIds(productIds).stream()
                .filter(p -> p.getIsEnable() != null && p.getIsEnable() == 1)
                .sorted(Comparator.comparing(Product::getSort, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        int total = products.size();
        int pages = (int) Math.ceil((double) total / pageSize);
        int from = (page - 1) * pageSize;
        int to = Math.min(from + pageSize, total);
        List<Product> pageItems = from < total ? products.subList(from, to) : List.of();

        List<GoodsDetailResponse> items = pageItems.stream()
                .map(homeController::buildGoodsDetail)
                .collect(Collectors.toList());

        return Result.ok(PageResult.of(items, total, page, pages, pageSize));
    }
}
