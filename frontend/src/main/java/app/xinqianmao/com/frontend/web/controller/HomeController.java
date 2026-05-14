/**
 * File: HomeController.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.frontend.web.controller;

import app.xinqianmao.com.common.annotation.NoAuth;
import app.xinqianmao.com.common.result.PageResult;
import app.xinqianmao.com.common.result.Result;
import app.xinqianmao.com.common.utils.ImageUrlUtil;
import app.xinqianmao.com.frontend.common.entity.*;
import app.xinqianmao.com.frontend.common.pojo.BannerResponse;
import app.xinqianmao.com.frontend.common.pojo.GoodsDetailResponse;
import app.xinqianmao.com.frontend.common.pojo.ShopDetailResponse;
import app.xinqianmao.com.frontend.dao.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Tag(name = "首页", description = "首页Banner和热门推荐")
@RestController
@RequestMapping("/frontend")
@RequiredArgsConstructor
public class HomeController {

    private final ShopMapper shopMapper;
    private final HotProductMapper hotProductMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper skuMapper;
    private final ProductPropertyMapper propertyMapper;
    private final ProductSpecsMapper specsMapper;
    private final ProductTypeSpecRelMapper typeSpecRelMapper;
    private final ImageUrlUtil imageUrlUtil;

    @NoAuth
    @Operation(summary = "获取店铺详情")
    @GetMapping("/shop/detail")
    public Result<ShopDetailResponse> shopDetail() {
        List<Shop> shops = shopMapper.selectList(new LambdaQueryWrapper<>());
        if (shops.isEmpty()) return Result.error("404", "店铺未配置");
        Shop shop = shops.get(0);
        ShopDetailResponse r = new ShopDetailResponse();
        r.setId(shop.getId());
        r.setName(shop.getName());
        r.setLogo(imageUrlUtil.fullUrl(shop.getLogo()));
        r.setFreeShippingAmount(shop.getFreeShippingAmount());
        String bannersJson = shop.getBanners();
        if (bannersJson != null && !bannersJson.isBlank()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode arr = mapper.readTree(bannersJson);
                if (arr.isArray()) {
                    List<ShopDetailResponse.BannerItem> items = new ArrayList<>();
                    for (int i = 0; i < arr.size(); i++) {
                        com.fasterxml.jackson.databind.JsonNode node = arr.get(i);
                        ShopDetailResponse.BannerItem bi = new ShopDetailResponse.BannerItem();
                        bi.setImgUrl(imageUrlUtil.fullUrl(node.path("imgUrl").asText("")));
                        bi.setHrefUrl(node.path("hrefUrl").asText("/pages/product/product?id=xxx"));
                        bi.setType(node.path("type").asInt(1));
                        bi.setSort(node.path("sort").asInt(i + 1));
                        items.add(bi);
                    }
                    r.setBanners(items);
                }
            } catch (Exception e) { log.warn("Failed to parse shop banners JSON: {}", e.getMessage()); }
        }
        return Result.ok(r);
    }

    @NoAuth
    @Operation(summary = "获取Banner轮播图（废弃，请用 /shop/detail）")
    @GetMapping("/home/banner")
    public Result<List<BannerResponse>> banners(@RequestParam(defaultValue = "1") Integer distributionSite) {
        List<Shop> shops = shopMapper.selectList(new LambdaQueryWrapper<>());
        if (shops.isEmpty()) return Result.ok(List.of());
        String bannersJson = shops.get(0).getBanners();
        if (bannersJson == null || bannersJson.isBlank()) return Result.ok(List.of());
        List<BannerResponse> result = new ArrayList<>();
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode arr = mapper.readTree(bannersJson);
            if (arr.isArray()) {
                for (int i = 0; i < arr.size(); i++) {
                    BannerResponse b = new BannerResponse();
                    b.setId("banner-" + (i + 1));
                    b.setImgUrl(imageUrlUtil.fullUrl(arr.get(i).path("imgUrl").asText("")));
                    b.setHrefUrl(arr.get(i).path("hrefUrl").asText("/pages/product/product?id=xxx"));
                    b.setType(distributionSite);
                    result.add(b);
                }
            }
        } catch (Exception e) { /* fallback */ }
        return Result.ok(result);
    }

    @NoAuth
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
                new LambdaQueryWrapper<Product>()
                        .in(Product::getId, productIds)
                        .eq(Product::getIsEnable, 1));
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
        r.setPicture(imageUrlUtil.fullUrl(product.getPicture()));
        r.setMainPictures(imageUrlUtil.fullUrls(product.getMainPictures()));

        // Details: properties + pictures + detail HTML
        GoodsDetailResponse.DetailInfo details = new GoodsDetailResponse.DetailInfo();
        List<ProductProperty> props = propertyMapper.selectList(
                new LambdaQueryWrapper<ProductProperty>().eq(ProductProperty::getProductId, product.getId())
                        .eq(ProductProperty::getIsDelete, 0));
        // Resolve spec names from specsId
        Map<String, String> specsNameMap = loadSpecsNameMap(props);
        details.setProperties(props.stream().map(prop -> {
            GoodsDetailResponse.PropertyItem pi = new GoodsDetailResponse.PropertyItem();
            pi.setName(specsNameMap.getOrDefault(prop.getSpecsId(), ""));
            pi.setValue(prop.getValueName());
            return pi;
        }).collect(Collectors.toList()));
        details.setPictures(new ArrayList<>());
        details.setDetail(imageUrlUtil.fullUrlsInHtml(product.getDetail()));
        r.setDetails(details);

        // SKUs
        List<ProductSku> skus = skuMapper.selectList(
                new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getProductId, product.getId())
                        .eq(ProductSku::getIsDelete, 0));
        r.setSkus(skus.stream().map(sku -> {
            GoodsDetailResponse.SkuItem gs = new GoodsDetailResponse.SkuItem();
            gs.setId(sku.getId());
            gs.setInventory(sku.getInventory());
            gs.setOldPrice(sku.getOldPrice());
            gs.setPicture(imageUrlUtil.fullUrl(sku.getPicture()));
            gs.setPrice(sku.getPrice());
            gs.setSkuCode("S" + sku.getId().substring(0, 8));
            gs.setSpecs(parseSpecValues(sku.getSpecs()));
            return gs;
        }).collect(Collectors.toList()));

        // Specs: include values from both spec definitions AND actual SKUs
        r.setSpecs(buildGoodsSpecs(getSpecsByProductType(product.getProductType()), skus));

        return r;
    }

    private Map<String, String> loadSpecsNameMap(List<ProductProperty> props) {
        if (props.isEmpty()) return Map.of();
        List<String> specsIds = props.stream().map(ProductProperty::getSpecsId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (specsIds.isEmpty()) return Map.of();
        Map<String, String> map = new HashMap<>();
        List<ProductSpecs> specsList = specsMapper.selectBatchIds(specsIds);
        specsList.forEach(s -> map.put(s.getId(), s.getName()));
        return map;
    }

    private List<ProductSpecs> getSpecsByProductType(String productType) {
        // Get type-linked specs
        List<ProductTypeSpecRel> rels = typeSpecRelMapper.selectList(
                new LambdaQueryWrapper<ProductTypeSpecRel>().eq(ProductTypeSpecRel::getProductType, productType));
        List<String> relatedSpecIds = rels.stream().map(ProductTypeSpecRel::getSpecsId).collect(Collectors.toList());
        // Also include global specs (scope=0)
        List<ProductSpecs> globalSpecs = specsMapper.selectList(
                new LambdaQueryWrapper<ProductSpecs>().eq(ProductSpecs::getScope, 0)
                        .orderByAsc(ProductSpecs::getSort));
        List<ProductSpecs> result = new ArrayList<>(globalSpecs);
        if (!relatedSpecIds.isEmpty()) {
            result.addAll(specsMapper.selectBatchIds(relatedSpecIds));
        }
        return result;
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

    public static List<GoodsDetailResponse.SpecItem> buildGoodsSpecs(List<ProductSpecs> specDefs, List<ProductSku> skus) {
        Map<String, Set<String>> grouped = new LinkedHashMap<>();
        for (ProductSpecs spec : specDefs) {
            if (spec.getType() != null && spec.getType() == 1) {
                grouped.computeIfAbsent(spec.getName(), k -> new LinkedHashSet<>())
                        .addAll(spec.getInputOptions() != null ? spec.getInputOptions() : List.of());
            }
        }
        // Also include values from actual SKU specs
        for (ProductSku sku : skus) {
            List<GoodsDetailResponse.SpecValue> skuSpecs = parseSpecValues(sku.getSpecs());
            for (GoodsDetailResponse.SpecValue sv : skuSpecs) {
                String specName = sv.getName();
                String valueName = sv.getValueName();
                if (specName != null && valueName != null && !valueName.isBlank()) {
                    grouped.computeIfAbsent(specName, k -> new LinkedHashSet<>()).add(valueName);
                }
            }
        }
        return grouped.entrySet().stream().map(entry -> {
            GoodsDetailResponse.SpecItem gs = new GoodsDetailResponse.SpecItem();
            gs.setName(entry.getKey());
            gs.setValues(entry.getValue().stream().map(v -> {
                GoodsDetailResponse.SpecValue svi = new GoodsDetailResponse.SpecValue();
                svi.setName(v); svi.setValueName(v); svi.setAvailable(true); svi.setDesc(v); svi.setPicture("");
                return svi;
            }).collect(Collectors.toList()));
            return gs;
        }).collect(Collectors.toList());
    }
}
