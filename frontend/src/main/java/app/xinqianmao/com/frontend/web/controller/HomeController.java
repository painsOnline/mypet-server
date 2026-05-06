/**
 * File: HomeController.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.frontend.web.controller;

import app.xinqianmao.com.common.result.PageResult;
import app.xinqianmao.com.common.result.Result;
import app.xinqianmao.com.frontend.common.entity.*;
import app.xinqianmao.com.frontend.common.pojo.BannerResponse;
import app.xinqianmao.com.frontend.common.pojo.GoodsDetailResponse;
import app.xinqianmao.com.frontend.dao.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "首页", description = "首页Banner和热门推荐")
@RestController
@RequiredArgsConstructor
public class HomeController {

    private final ShopMapper shopMapper;
    private final HotProductMapper hotProductMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper skuMapper;
    private final ProductPropertyMapper propertyMapper;
    private final ProductSpecsMapper specsMapper;

    @Operation(summary = "获取Banner轮播图")
    @GetMapping("/home/banner")
    public Result<List<BannerResponse>> banners(@RequestParam(defaultValue = "1") Integer distributionSite) {
        List<Shop> shops = shopMapper.selectList(new LambdaQueryWrapper<>());
        if (shops.isEmpty()) return Result.ok(List.of());
        List<String> bannerUrls = shops.get(0).getBanners();
        if (bannerUrls == null || bannerUrls.isEmpty()) return Result.ok(List.of());
        List<BannerResponse> result = new ArrayList<>();
        for (int i = 0; i < bannerUrls.size(); i++) {
            BannerResponse b = new BannerResponse();
            b.setId("banner-" + (i + 1));
            b.setImgUrl(bannerUrls.get(i));
            b.setHrefUrl("/pages/product/product?id=xxx");
            b.setType(distributionSite);
            result.add(b);
        }
        return Result.ok(result);
    }

    @Operation(summary = "热门推荐商品列表")
    @GetMapping("/home/hot")
    public Result<PageResult<GoodsDetailResponse>> hotProducts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "6") Integer pageSize) {
        List<HotProduct> hotList = hotProductMapper.selectList(
                new LambdaQueryWrapper<HotProduct>().orderByAsc(HotProduct::getSort));
        List<String> productIds = hotList.stream().map(HotProduct::getProductId).distinct().toList();
        if (productIds.isEmpty()) {
            return Result.ok(PageResult.of(List.of(), 0, page, 0, pageSize));
        }
        Page<Product> p = Page.of(page, pageSize);
        IPage<Product> productPage = productMapper.selectPage(p,
                new LambdaQueryWrapper<Product>().in(Product::getId, productIds));
        List<GoodsDetailResponse> items = productPage.getRecords().stream()
                .map(this::buildGoodsDetail).collect(Collectors.toList());
        return Result.ok(PageResult.of(items, productPage.getTotal(), page, productPage.getPages(), pageSize));
    }

    GoodsDetailResponse buildGoodsDetail(Product product) {
        GoodsDetailResponse r = new GoodsDetailResponse();
        r.setId(product.getId());
        r.setName(product.getName());
        r.setDesc(product.getDesc());
        r.setPrice(product.getPrice());
        r.setOldPrice(product.getOldPrice());
        r.setPicture(product.getPicture());
        r.setMainPictures(product.getMainPictures() != null ? product.getMainPictures() : List.of());

        // Details: properties + pictures from detail (treat detail as pictures list)
        GoodsDetailResponse.DetailInfo details = new GoodsDetailResponse.DetailInfo();
        List<ProductProperty> props = propertyMapper.selectList(
                new LambdaQueryWrapper<ProductProperty>().eq(ProductProperty::getProductId, product.getId()));
        details.setProperties(props.stream().map(prop -> {
            GoodsDetailResponse.PropertyItem pi = new GoodsDetailResponse.PropertyItem();
            pi.setName(prop.getName());
            pi.setValue(prop.getValueName());
            return pi;
        }).collect(Collectors.toList()));
        details.setPictures(new ArrayList<>());
        r.setDetails(details);

        // SKUs
        List<ProductSku> skus = skuMapper.selectList(
                new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getProductId, product.getId()));
        r.setSkus(skus.stream().map(sku -> {
            GoodsDetailResponse.SkuItem gs = new GoodsDetailResponse.SkuItem();
            gs.setId(sku.getId());
            gs.setInventory(sku.getInventory());
            gs.setOldPrice(sku.getOldPrice());
            gs.setPicture(sku.getPicture());
            gs.setPrice(sku.getPrice());
            gs.setSkuCode("S" + sku.getId().substring(0, 8));
            gs.setSpecs(parseSpecValues(sku.getSpecs()));
            return gs;
        }).collect(Collectors.toList()));

        // Specs grouped
        List<ProductSpecs> specDefs = specsMapper.selectList(
                new LambdaQueryWrapper<ProductSpecs>().eq(ProductSpecs::getProductType, product.getProductType()));
        r.setSpecs(buildGoodsSpecs(specDefs));

        return r;
    }

    public static List<GoodsDetailResponse.SpecValue> parseSpecValues(String specsJson) {
        if (specsJson == null || specsJson.isBlank()) return List.of();
        try {
            ObjectMapper mapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            List<Map<String, String>> list = mapper.readValue(specsJson, List.class);
            return list.stream().map(m -> {
                GoodsDetailResponse.SpecValue sv = new GoodsDetailResponse.SpecValue();
                sv.setName(m.get("name"));
                sv.setValueName(m.get("valueName"));
                return sv;
            }).collect(Collectors.toList());
        } catch (Exception e) { return List.of(); }
    }

    public static List<GoodsDetailResponse.SpecItem> buildGoodsSpecs(List<ProductSpecs> specDefs) {
        Map<String, List<String>> grouped = new LinkedHashMap<>();
        for (ProductSpecs spec : specDefs) {
            if (spec.getType() != null && spec.getType() == 1) {
                grouped.computeIfAbsent(spec.getName(), k -> new ArrayList<>())
                        .addAll(spec.getInputOptions() != null ? spec.getInputOptions() : List.of());
            }
        }
        return grouped.entrySet().stream().map(entry -> {
            GoodsDetailResponse.SpecItem gs = new GoodsDetailResponse.SpecItem();
            gs.setName(entry.getKey());
            gs.setValues(entry.getValue().stream().distinct().map(v -> {
                GoodsDetailResponse.SpecValue svi = new GoodsDetailResponse.SpecValue();
                svi.setName(v); svi.setAvailable(true); svi.setDesc(v); svi.setPicture("");
                return svi;
            }).collect(Collectors.toList()));
            return gs;
        }).collect(Collectors.toList());
    }
}
