/**
 * File: HomeService.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.frontend.service;

import app.xinqianmao.com.common.utils.DateTimeUtil;
import app.xinqianmao.com.frontend.common.entity.*;
import app.xinqianmao.com.frontend.common.pojo.BannerResponse;
import app.xinqianmao.com.frontend.common.pojo.GoodsDetailResponse;
import app.xinqianmao.com.frontend.dao.*;
import app.xinqianmao.com.frontend.dao.ProductSpecsValueMapper;
import app.xinqianmao.com.frontend.common.entity.ProductSpecsValue;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Home page service: banners, hot products.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HomeService {

    private final ShopMapper shopMapper;
    private final HotProductMapper hotProductMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final ProductPropertyMapper productPropertyMapper;
    private final ProductSpecsMapper productSpecsMapper;
    private final ProductTypeSpecRelMapper productTypeSpecRelMapper;
    private final ProductSpecsValueMapper specsValueMapper;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Get banner list from t_shop banners field.
     */
    public List<BannerResponse> getBanners() {
        List<Shop> shops = shopMapper.selectList(new LambdaQueryWrapper<>());
        if (shops.isEmpty() || shops.get(0).getBanners() == null) {
            return List.of();
        }
        Shop shop = shops.get(0);
        List<BannerResponse> banners = new ArrayList<>();
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode arr = mapper.readTree(shop.getBanners());
            if (arr.isArray()) {
                for (int i = 0; i < arr.size(); i++) {
                    BannerResponse banner = new BannerResponse();
                    banner.setId("banner-" + (i + 1));
                    banner.setImgUrl(arr.get(i).path("imgUrl").asText(""));
                    banner.setHrefUrl(arr.get(i).path("hrefUrl").asText("/pages/product/product?id=xxx"));
                    banner.setType(arr.get(i).path("type").asInt(1));
                    banners.add(banner);
                }
            }
        } catch (Exception e) { /* ignore */ }
        return banners;
    }

    /**
     * Get hot products with pagination. Joins hot_products -> product -> sku/properties/specs.
     */
    public IPage<GoodsDetailResponse> getHotProducts(int page, int pageSize) {
        // Query all hot products sorted by sort
        List<HotProduct> hotList = hotProductMapper.selectList(
                new LambdaQueryWrapper<HotProduct>().orderByAsc(HotProduct::getSort));
        if (hotList.isEmpty()) {
            Page<GoodsDetailResponse> emptyPage = new Page<>(page, pageSize);
            emptyPage.setRecords(List.of());
            emptyPage.setTotal(0);
            return emptyPage;
        }

        // Collect all product IDs from hot list
        List<String> productIds = hotList.stream()
                .map(HotProduct::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (productIds.isEmpty()) {
            Page<GoodsDetailResponse> emptyPage = new Page<>(page, pageSize);
            emptyPage.setRecords(List.of());
            emptyPage.setTotal(0);
            return emptyPage;
        }

        // Load products in hot order
        List<Product> allProducts = productMapper.selectBatchIds(productIds);
        Map<String, Product> productMap = allProducts.stream()
                .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a, LinkedHashMap::new));

        // Order products by hot sort
        List<Product> orderedProducts = hotList.stream()
                .map(hp -> productMap.get(hp.getProductId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Manual pagination
        int total = orderedProducts.size();
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);

        List<GoodsDetailResponse> records;
        if (fromIndex >= total) {
            records = List.of();
        } else {
            List<Product> pageProducts = orderedProducts.subList(fromIndex, toIndex);
            // Preload SKUs, properties, specs for this page
            Set<String> pageProductIds = pageProducts.stream().map(Product::getId).collect(Collectors.toSet());

            Map<String, List<ProductSku>> skusByProduct = loadSkusByProductIds(pageProductIds);
            Map<String, List<ProductProperty>> propsByProduct = loadPropertiesByProductIds(pageProductIds);
            Map<String, List<ProductSpecs>> specsByProduct = loadSpecsByProductIds(pageProductIds, productMap);

            records = pageProducts.stream()
                    .map(p -> buildGoodsDetail(p, skusByProduct.getOrDefault(p.getId(), List.of()),
                            propsByProduct.getOrDefault(p.getId(), List.of()),
                            specsByProduct.getOrDefault(p.getId(), List.of())))
                    .collect(Collectors.toList());
        }

        Page<GoodsDetailResponse> resultPage = new Page<>(page, pageSize);
        resultPage.setRecords(records);
        resultPage.setTotal(total);
        return resultPage;
    }

    // --- Helper methods ---

    private Map<String, List<ProductSku>> loadSkusByProductIds(Set<String> productIds) {
        if (productIds.isEmpty()) return Map.of();
        List<ProductSku> skus = productSkuMapper.selectList(
                new LambdaQueryWrapper<ProductSku>().in(ProductSku::getProductId, productIds)
                        .eq(ProductSku::getIsDelete, 0));
        return skus.stream().collect(Collectors.groupingBy(ProductSku::getProductId));
    }

    private Map<String, List<ProductProperty>> loadPropertiesByProductIds(Set<String> productIds) {
        if (productIds.isEmpty()) return Map.of();
        List<ProductProperty> props = productPropertyMapper.selectList(
                new LambdaQueryWrapper<ProductProperty>().in(ProductProperty::getProductId, productIds)
                        .eq(ProductProperty::getIsDelete, 0)
                        .orderByAsc(ProductProperty::getSort));
        return props.stream().collect(Collectors.groupingBy(ProductProperty::getProductId));
    }

    private Map<String, List<ProductSpecs>> loadSpecsByProductIds(Set<String> productIds, Map<String, Product> productMap) {
        if (productIds.isEmpty()) return Map.of();
        Set<String> typeIds = productIds.stream()
                .map(id -> productMap.get(id))
                .filter(Objects::nonNull)
                .map(Product::getProductType)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (typeIds.isEmpty()) return Map.of();
        // Get specs via ProductTypeSpecRel
        List<ProductTypeSpecRel> rels = productTypeSpecRelMapper.selectList(
                new LambdaQueryWrapper<ProductTypeSpecRel>().in(ProductTypeSpecRel::getProductType, typeIds));
        Map<String, List<String>> specsIdsByType = rels.stream()
                .collect(Collectors.groupingBy(ProductTypeSpecRel::getProductType,
                        Collectors.mapping(ProductTypeSpecRel::getSpecsId, Collectors.toList())));
        // Also include global specs (scope=0)
        List<ProductSpecs> globalSpecs = productSpecsMapper.selectList(
                new LambdaQueryWrapper<ProductSpecs>().eq(ProductSpecs::getScope, 0)
                        .orderByAsc(ProductSpecs::getSort));
        Map<String, List<ProductSpecs>> specsByProduct = new LinkedHashMap<>();
        for (String pid : productIds) {
            Product p = productMap.get(pid);
            if (p == null) continue;
            List<ProductSpecs> list = new ArrayList<>(globalSpecs);
            List<String> ids = specsIdsByType.get(p.getProductType());
            if (ids != null && !ids.isEmpty()) {
                list.addAll(productSpecsMapper.selectBatchIds(ids));
            }
            specsByProduct.put(pid, list);
        }
        return specsByProduct;
    }

    private GoodsDetailResponse buildGoodsDetail(Product product, List<ProductSku> skus,
                                                  List<ProductProperty> properties,
                                                  List<ProductSpecs> specs) {
        GoodsDetailResponse r = new GoodsDetailResponse();
        r.setId(product.getId());
        r.setName(product.getName());
        r.setDesc(product.getDesc());
        r.setPrice(product.getPrice());
        r.setOldPrice(product.getOldPrice());
        r.setPicture(product.getPicture());
        r.setMainPictures(product.getMainPictures());

        // Build specs name map for property name resolution
        Map<String, String> specsNameMap = specs.stream()
                .filter(s -> s.getId() != null && s.getName() != null)
                .collect(Collectors.toMap(ProductSpecs::getId, ProductSpecs::getName, (a, b) -> a));

        // Load value_id -> value_name map for all referenced value_ids (properties + SKU specs)
        Map<String, String> valueIdToName = new HashMap<>();
        java.util.Set<String> vids = properties.stream().map(ProductProperty::getValueId)
            .filter(v -> v != null && !v.isBlank()).collect(Collectors.toSet());
        for (ProductSku sku : skus) {
            String specsJson = sku.getSpecs();
            if (specsJson != null && !specsJson.isBlank() && !"[]".equals(specsJson)) {
                try {
                    List<Map<String, String>> parsed = OBJECT_MAPPER.readValue(specsJson,
                            new TypeReference<List<Map<String, String>>>() {});
                    for (Map<String, String> m : parsed) {
                        String vid = m.get("value_id");
                        if (vid != null && !vid.isBlank()) vids.add(vid);
                    }
                } catch (Exception ignored) {}
            }
        }
        if (!vids.isEmpty()) {
            specsValueMapper.selectList(new LambdaQueryWrapper<ProductSpecsValue>().in(ProductSpecsValue::getId, vids))
                .forEach(sv -> valueIdToName.put(sv.getId(), sv.getValueName()));
        }

        // Details block
        GoodsDetailResponse.DetailInfo details = new GoodsDetailResponse.DetailInfo();
        details.setProperties(properties.stream().map(prop -> {
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
        // Parse detail text as pictures (comma or newline separated)
        details.setPictures(parseDetailPictures(product.getDetail()));
        r.setDetails(details);

        Map<String, String> specIdToName = new HashMap<>();
        for (ProductSpecs s : specs) specIdToName.put(s.getId(), s.getName());

        // SKU list — resolve spec_name and value_name dynamically
        r.setSkus(skus.stream().map(sku -> {
            GoodsDetailResponse.SkuItem si = new GoodsDetailResponse.SkuItem();
            si.setId(sku.getId());
            si.setInventory(sku.getInventory());
            si.setOldPrice(sku.getOldPrice());
            si.setPicture(sku.getPicture());
            si.setPrice(sku.getPrice());
            si.setSpecs(parseSpecsJson(sku.getSpecs(), specIdToName, valueIdToName));
            return si;
        }).collect(Collectors.toList()));

        // Specs for SKU selection UI — resolve value_name, set sort/inputType
        r.setSpecs(buildSpecItems(specs, skus, valueIdToName));

        return r;
    }

    /**
     * Parse detail text into list of picture URLs. If detail looks like JSON array, parse it;
     * otherwise split by newlines and return non-blank lines.
     */
    private List<String> parseDetailPictures(String detail) {
        if (detail == null || detail.isBlank()) return List.of();
        String trimmed = detail.strip();
        if (trimmed.startsWith("[")) {
            try {
                return OBJECT_MAPPER.readValue(trimmed, new TypeReference<List<String>>() {});
            } catch (Exception e) {
                // fall through to line split
            }
        }
        return Arrays.stream(trimmed.split("\\n"))
                .map(String::strip)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Parse specs JSON string into list of SpecValue.
     */
    @SuppressWarnings("unchecked")
    private List<GoodsDetailResponse.SpecValue> parseSpecsJson(String specsJson, Map<String, String> specIdToName,
                                                                Map<String, String> valueIdToName) {
        if (specsJson == null || specsJson.isBlank()) return List.of();
        try {
            List<Map<String, String>> list = OBJECT_MAPPER.readValue(specsJson,
                    new TypeReference<List<Map<String, String>>>() {});
            return list.stream().map(m -> {
                GoodsDetailResponse.SpecValue sv = new GoodsDetailResponse.SpecValue();
                String sid = m.get("spec_id");
                sv.setSpecId(sid);
                sv.setSpecName(specIdToName != null && sid != null ? specIdToName.get(sid) : null);
                String vn = m.get("value_name");
                String vid = m.get("value_id");
                if ((vn == null || vn.isBlank()) && vid != null && valueIdToName != null) {
                    vn = valueIdToName.get(vid);
                }
                sv.setValueName(vn);
                sv.setValueId(vid);
                return sv;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Build spec items for SKU selection UI from spec definitions and actual SKU values.
     */
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

    private List<GoodsDetailResponse.SpecItem> buildSpecItems(List<ProductSpecs> specDefs, List<ProductSku> skus,
                                                               Map<String, String> valueIdToName) {
        Map<String, String> specIdToName = new HashMap<>();
        Map<String, ProductSpecs> specDefMap = new HashMap<>();
        for (ProductSpecs s : specDefs) {
            if (s.getId() != null) {
                specIdToName.put(s.getId(), s.getName());
                specDefMap.put(s.getId(), s);
            }
        }
        Map<String, GoodsDetailResponse.SpecItem> specMap = new LinkedHashMap<>();
        Map<String, Map<String, GoodsDetailResponse.SpecValue>> specValues = new LinkedHashMap<>();
        for (ProductSku sku : skus) {
            List<GoodsDetailResponse.SpecValue> svList = parseSpecsJson(sku.getSpecs(), specIdToName, valueIdToName);
            for (GoodsDetailResponse.SpecValue sv : svList) {
                if (sv.getSpecId() == null) continue;
                final String sn = specIdToName.getOrDefault(sv.getSpecId(),
                    sv.getSpecName() != null ? sv.getSpecName() : "");
                ProductSpecs specDef = specDefMap.get(sv.getSpecId());
                specMap.computeIfAbsent(sv.getSpecId(), k -> {
                    GoodsDetailResponse.SpecItem si = new GoodsDetailResponse.SpecItem();
                    si.setSpecId(sv.getSpecId());
                    si.setSpecName(sn);
                    if (specDef != null) {
                        si.setSort(specDef.getSort());
                        si.setInputType(specDef.getInputType());
                    }
                    return si;
                });
                Map<String, GoodsDetailResponse.SpecValue> vals = specValues.computeIfAbsent(sn, k -> new LinkedHashMap<>());
                sv.setAvailable(true);
                vals.putIfAbsent(sv.getValueName(), sv);
            }
        }
        return new ArrayList<>(specMap.values().stream().peek(si ->
            si.setValues(new ArrayList<>(specValues.getOrDefault(si.getSpecName(), Map.of()).values()))
        ).toList());
    }
}
