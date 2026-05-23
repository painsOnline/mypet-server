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
import app.xinqianmao.com.frontend.common.entity.ProductSku;
import app.xinqianmao.com.frontend.common.pojo.GoodsDetailResponse;
import app.xinqianmao.com.frontend.dao.ProductMapper;
import app.xinqianmao.com.frontend.dao.ProductSkuMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Tag(name = "搜索", description = "商品全文搜索")
@RestController
@RequestMapping("/frontend/search")
@RequiredArgsConstructor
public class SearchController {

    private final ProductMapper productMapper;
    private final ProductSkuMapper skuMapper;
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
        Set<String> seenIds = new LinkedHashSet<>();
        List<Product> merged = new ArrayList<>();

        log.info("=== Search keyword: [{}] ===");

        // 1. Jieba full-text search on search_text
        try {
            // Debug: check how jieba segments the keyword
            LambdaQueryWrapper<Product> w1 = new LambdaQueryWrapper<>();
            w1.apply("to_tsvector('jiebacfg', search_text) @@ plainto_tsquery('jiebacfg', {0})", kw)
              .eq(Product::getIsEnable, 1)
              .last("ORDER BY ts_rank(to_tsvector('jiebacfg', search_text), plainto_tsquery('jiebacfg', '" + escapeSql(kw) + "')) DESC, sort ASC");
            List<Product> r1 = productMapper.selectList(w1);
            log.info("Jieba result count: {}", r1.size());
            r1.forEach(p -> { if (seenIds.add(p.getId())) merged.add(p); });
        } catch (Exception e) { log.warn("Jieba search failed: {}", e.getMessage()); }

        // 2. Trigram similarity on search_text
        try {
            LambdaQueryWrapper<Product> w2 = new LambdaQueryWrapper<>();
            w2.apply("similarity(search_text, {0}) > 0.05", kw)
              .eq(Product::getIsEnable, 1)
              .last("ORDER BY similarity(search_text, '" + escapeSql(kw) + "') DESC, sort ASC");
            List<Product> r2 = productMapper.selectList(w2);
            log.info("Trigram result count: {}", r2.size());
            r2.forEach(p -> { if (seenIds.add(p.getId())) merged.add(p); });
        } catch (Exception e) { log.warn("Trigram search failed: {}", e.getMessage()); }

        // Debug: check if search_text has any data at all
        try {
            LambdaQueryWrapper<Product> wd = new LambdaQueryWrapper<>();
            wd.isNotNull(Product::getSearchText).eq(Product::getIsEnable, 1)
              .last("LIMIT 3");
            List<Product> sample = productMapper.selectList(wd);
            log.info("Products with non-null search_text count (sample): {}", sample.size());
            for (Product p : sample) {
                log.info("  id={}, name=[{}], searchText=[{}]", p.getId(), p.getName(),
                         p.getSearchText() != null ? p.getSearchText().substring(0, Math.min(100, p.getSearchText().length())) : "NULL");
            }
        } catch (Exception e) { log.warn("Debug query failed: {}", e.getMessage()); }

        log.info("=== Merged before LIKE fallbacks: {} ===", merged.size());

        // 3. Direct LIKE on search_text (most reliable fallback) — 暂时注释，先验证分词+模糊效果
        // try {
        //     productMapper.selectList(new LambdaQueryWrapper<Product>()
        //             .like(Product::getSearchText, kw)
        //             .eq(Product::getIsEnable, 1)
        //             .orderByAsc(Product::getSort))
        //             .forEach(p -> { if (seenIds.add(p.getId())) merged.add(p); });
        // } catch (Exception e) { log.warn("search_text LIKE failed: {}", e.getMessage()); }

        // 4. LIKE on product name (always works regardless of search_text) — 暂时注释，先验证分词+模糊效果
        // try {
        //     productMapper.selectList(new LambdaQueryWrapper<Product>()
        //             .like(Product::getName, kw)
        //             .eq(Product::getIsEnable, 1)
        //             .orderByAsc(Product::getSort))
        //             .forEach(p -> { if (seenIds.add(p.getId())) merged.add(p); });
        // } catch (Exception e) { log.warn("Name LIKE failed: {}", e.getMessage()); }

        // 5. LIKE on product desc — 暂时注释，先验证分词+模糊效果
        // try {
        //     productMapper.selectList(new LambdaQueryWrapper<Product>()
        //             .like(Product::getDesc, kw)
        //             .eq(Product::getIsEnable, 1)
        //             .orderByAsc(Product::getSort))
        //             .forEach(p -> { if (seenIds.add(p.getId())) merged.add(p); });
        // } catch (Exception e) { log.warn("Desc LIKE failed: {}", e.getMessage()); }

        if (merged.isEmpty()) {
            return Result.ok(PageResult.of(List.of(), 0, page, 0, pageSize));
        }

        // Stock-aware sorting: in-stock products first
        Set<String> inStockProductIds = getInStockProductIds(
                merged.stream().map(Product::getId).collect(Collectors.toList()));

        merged.sort(Comparator
                .comparing((Product p) -> !inStockProductIds.contains(p.getId()))
                .thenComparing(Product::getSort, Comparator.nullsLast(Comparator.naturalOrder())));

        // Paginate
        int total = merged.size();
        int pages = (int) Math.ceil((double) total / pageSize);
        int from = (page - 1) * pageSize;
        int to = Math.min(from + pageSize, total);
        List<Product> pageItems = from < total ? merged.subList(from, to) : List.of();

        List<GoodsDetailResponse> items = pageItems.stream()
                .map(homeController::buildGoodsDetail)
                .collect(Collectors.toList());

        return Result.ok(PageResult.of(items, total, page, pages, pageSize));
    }

    private Set<String> getInStockProductIds(List<String> productIds) {
        if (productIds.isEmpty()) return Set.of();
        Set<String> result = new HashSet<>();
        for (String pid : productIds) {
            Long count = skuMapper.selectCount(new LambdaQueryWrapper<ProductSku>()
                    .eq(ProductSku::getProductId, pid)
                    .eq(ProductSku::getIsDelete, 0)
                    .gt(ProductSku::getInventory, 0));
            if (count > 0) result.add(pid);
        }
        return result;
    }

    private String escapeSql(String s) {
        return s.replaceAll("['\\\\]", "\\\\$0");
    }
}
