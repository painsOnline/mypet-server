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
import app.xinqianmao.com.frontend.dao.ProductSpecsValueMapper;
import app.xinqianmao.com.frontend.common.entity.ProductSpecsValue;
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
    private final ProductSpecsValueMapper specsValueMapper;
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
        Map<String, String> valueIdToName = new HashMap<>();
        java.util.Set<String> vids = props.stream().map(ProductProperty::getValueId)
            .filter(v -> v != null && !v.isBlank()).collect(Collectors.toSet());
        if (!vids.isEmpty()) {
            specsValueMapper.selectList(new LambdaQueryWrapper<ProductSpecsValue>().in(ProductSpecsValue::getId, vids))
                .forEach(sv -> valueIdToName.put(sv.getId(), sv.getValueName()));
        }
        details.setProperties(props.stream().map(prop -> {
            GoodsDetailResponse.PropertyItem pi = new GoodsDetailResponse.PropertyItem();
            pi.setSpecId(prop.getSpecsId());
            pi.setSpecName(specsNameMap.getOrDefault(prop.getSpecsId(), ""));
            pi.setValueId(prop.getValueId());
            String vn = prop.getValueName();
            if ((vn == null || vn.isBlank()) && prop.getValueId() != null) {
                vn = valueIdToName.getOrDefault(prop.getValueId(), "");
            }
            pi.setValueName(vn);
            return pi;
        }).collect(Collectors.toList()));
        details.setPictures(new ArrayList<>());
        details.setDetail(imageUrlUtil.fullUrlsInHtml(product.getDetail()));
        r.setDetails(details);

        // SKUs
        List<ProductSku> skus = skuMapper.selectList(
                new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getProductId, product.getId())
                        .eq(ProductSku::getIsDelete, 0));
        // Load specDefs first for specId→specName resolution
        List<ProductSpecs> specDefs = getSpecsByProductType(product.getProductType());
        Map<String, String> specIdToName = new HashMap<>();
        for (ProductSpecs s : specDefs) specIdToName.put(s.getId(), s.getName());

        r.setSkus(skus.stream().map(sku -> {
            GoodsDetailResponse.SkuItem gs = new GoodsDetailResponse.SkuItem();
            gs.setId(sku.getId());
            gs.setInventory(sku.getInventory());
            gs.setOldPrice(sku.getOldPrice());
            gs.setPicture(imageUrlUtil.fullUrl(sku.getPicture()));
            gs.setPrice(sku.getPrice());
            gs.setSpecs(parseSpecValues(sku.getSpecs(), specIdToName));
            return gs;
        }).collect(Collectors.toList()));

        r.setSpecs(buildSpecItems(specDefs, skus));

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

    public static List<GoodsDetailResponse.SpecValue> parseSpecValues(String specsJson, Map<String, String> specIdToName) {
        if (specsJson == null || specsJson.isBlank()) return List.of();
        try {
            ObjectMapper mapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            List<Map<String, String>> list = mapper.readValue(specsJson, List.class);
            return list.stream().map(m -> {
                GoodsDetailResponse.SpecValue sv = new GoodsDetailResponse.SpecValue();
                sv.setSpecId(m.get("spec_id"));
                sv.setSpecName(m.get("spec_name"));
                sv.setValueId(m.get("value_id"));
                sv.setValueName(m.get("value_name"));
                return sv;
            }).collect(Collectors.toList());
        } catch (Exception e) { return List.of(); }
    }

    private void loadSpecValues(List<ProductSpecs> specs) {
        if (specs.isEmpty()) return;
        List<String> ids = specs.stream().map(ProductSpecs::getId).collect(Collectors.toList());
        List<ProductSpecsValue> vals = specsValueMapper.selectList(
            new LambdaQueryWrapper<ProductSpecsValue>().in(ProductSpecsValue::getSpecsId, ids)
                    .orderByAsc(ProductSpecsValue::getSort));
        Map<String, List<ProductSpecsValue>> map = new HashMap<>();
        for (ProductSpecsValue v : vals) {
            map.computeIfAbsent(v.getSpecsId(), k -> new ArrayList<>()).add(v);
        }
        for (ProductSpecs s : specs) {
            s.setValuesList(map.getOrDefault(s.getId(), List.of()));
            // Also populate inputOptions for backward compatibility
            s.setInputOptions(map.getOrDefault(s.getId(), List.of()).stream()
                    .map(ProductSpecsValue::getValueName).collect(Collectors.toList()));
        }
    }

    private List<GoodsDetailResponse.SpecItem> buildSpecItems(List<ProductSpecs> specDefs, List<ProductSku> skus) {
        Map<String, GoodsDetailResponse.SpecItem> specMap = new LinkedHashMap<>();
        Map<String, Map<String, GoodsDetailResponse.SpecValue>> specValues = new LinkedHashMap<>();
        for (ProductSku sku : skus) {
            List<GoodsDetailResponse.SpecValue> svList = parseSpecValues(sku.getSpecs(), null);
            for (GoodsDetailResponse.SpecValue sv : svList) {
                if (sv.getSpecId() == null || sv.getSpecName() == null) continue;
                specMap.computeIfAbsent(sv.getSpecId(), k -> {
                    GoodsDetailResponse.SpecItem si = new GoodsDetailResponse.SpecItem();
                    si.setSpecId(sv.getSpecId());
                    si.setSpecName(sv.getSpecName());
                    return si;
                });
                Map<String, GoodsDetailResponse.SpecValue> vals = specValues.computeIfAbsent(
                    sv.getSpecName(), k -> new LinkedHashMap<>());
                sv.setAvailable(true);
                vals.putIfAbsent(sv.getValueName(), sv);
            }
        }
        return specMap.values().stream().map(si -> {
            si.setValues(new ArrayList<>(specValues.getOrDefault(si.getSpecName(), Map.of()).values()));
            return si;
        }).collect(Collectors.toList());
    }
}
