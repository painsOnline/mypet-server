/**
 * File: CategoryService.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.frontend.service;

import app.xinqianmao.com.common.exception.BizException;
import app.xinqianmao.com.frontend.common.entity.*;
import app.xinqianmao.com.frontend.common.pojo.GoodsDetailResponse;
import app.xinqianmao.com.frontend.dao.*;
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
 * Category service: list categories, products by category.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final ProductCategoryMapper productCategoryMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final ProductPropertyMapper productPropertyMapper;
    private final ProductSpecsMapper productSpecsMapper;
    private final ProductTypeSpecRelMapper productTypeSpecRelMapper;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * List all product categories sorted by sort order.
     */
    public List<ProductCategory> listAll() {
        return productCategoryMapper.selectList(
                new LambdaQueryWrapper<ProductCategory>().orderByAsc(ProductCategory::getSort));
    }

    /**
     * Get products filtered by category with pagination.
     * Returns IPage<GoodsDetailResponse> with the same structure as hot products.
     */
    public IPage<GoodsDetailResponse> getProductsByCategory(String categoryId, int page, int pageSize) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getProductCategory, categoryId)
                .orderByAsc(Product::getSort)
                .orderByDesc(Product::getCreateTime);

        Page<Product> productPage = Page.of(page, pageSize);
        IPage<Product> pagedProducts = productMapper.selectPage(productPage, wrapper);
        List<Product> products = pagedProducts.getRecords();

        if (products.isEmpty()) {
            return pagedProducts.convert(p -> null);
        }

        // Preload related data
        Set<String> productIds = products.stream().map(Product::getId).collect(Collectors.toSet());
        Map<String, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a, LinkedHashMap::new));
        Map<String, List<ProductSku>> skusByProduct = loadSkusByProductIds(productIds);
        Map<String, List<ProductProperty>> propsByProduct = loadPropertiesByProductIds(productIds);
        Map<String, List<ProductSpecs>> specsByProduct = loadSpecsByProductIds(productIds, productMap);

        return pagedProducts.convert(p -> buildGoodsDetail(p,
                skusByProduct.getOrDefault(p.getId(), List.of()),
                propsByProduct.getOrDefault(p.getId(), List.of()),
                specsByProduct.getOrDefault(p.getId(), List.of())));
    }

    // --- Helper methods (same pattern as HomeService) ---

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
                .map(productMap::get)
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

        GoodsDetailResponse.DetailInfo details = new GoodsDetailResponse.DetailInfo();
        details.setProperties(properties.stream().map(prop -> {
            GoodsDetailResponse.PropertyItem pi = new GoodsDetailResponse.PropertyItem();
            pi.setName(specsNameMap.getOrDefault(prop.getSpecsId(), ""));
            pi.setValue(prop.getValueName());
            return pi;
        }).collect(Collectors.toList()));
        details.setPictures(parseDetailPictures(product.getDetail()));
        r.setDetails(details);

        r.setSkus(skus.stream().map(sku -> {
            GoodsDetailResponse.SkuItem si = new GoodsDetailResponse.SkuItem();
            si.setId(sku.getId());
            si.setInventory(sku.getInventory());
            si.setOldPrice(sku.getOldPrice());
            si.setPicture(sku.getPicture());
            si.setPrice(sku.getPrice());
            si.setSkuCode(sku.getId());
            si.setSpecs(parseSpecsJson(sku.getSpecs()));
            return si;
        }).collect(Collectors.toList()));

        r.setSpecs(buildSpecItems(specs, skus));
        return r;
    }

    private List<String> parseDetailPictures(String detail) {
        if (detail == null || detail.isBlank()) return List.of();
        String trimmed = detail.strip();
        if (trimmed.startsWith("[")) {
            try {
                return OBJECT_MAPPER.readValue(trimmed, new TypeReference<List<String>>() {});
            } catch (Exception e) {
                // fall through
            }
        }
        return Arrays.stream(trimmed.split("\\n"))
                .map(String::strip)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private List<GoodsDetailResponse.SpecValue> parseSpecsJson(String specsJson) {
        if (specsJson == null || specsJson.isBlank()) return List.of();
        try {
            List<Map<String, String>> list = OBJECT_MAPPER.readValue(specsJson,
                    new TypeReference<List<Map<String, String>>>() {});
            return list.stream().map(m -> {
                GoodsDetailResponse.SpecValue sv = new GoodsDetailResponse.SpecValue();
                sv.setName(m.get("name"));
                sv.setValueName(m.get("valueName"));
                return sv;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<GoodsDetailResponse.SpecItem> buildSpecItems(List<ProductSpecs> specDefs, List<ProductSku> skus) {
        Set<String> allNames = new LinkedHashSet<>();
        for (ProductSpecs spec : specDefs) {
            if (spec.getType() != null && spec.getType() == 1) {
                allNames.add(spec.getName());
            }
        }

        Set<String> availableValueKeys = new HashSet<>();
        for (ProductSku sku : skus) {
            List<GoodsDetailResponse.SpecValue> svList = parseSpecsJson(sku.getSpecs());
            for (GoodsDetailResponse.SpecValue sv : svList) {
                availableValueKeys.add(sv.getName() + "::" + sv.getValueName());
            }
        }

        List<GoodsDetailResponse.SpecItem> items = new ArrayList<>();
        for (String name : allNames) {
            GoodsDetailResponse.SpecItem item = new GoodsDetailResponse.SpecItem();
            item.setName(name);

            Set<String> optionSet = new LinkedHashSet<>();
            for (ProductSpecs spec : specDefs) {
                if (name.equals(spec.getName()) && spec.getInputOptions() != null) {
                    optionSet.addAll(spec.getInputOptions());
                }
            }

            List<GoodsDetailResponse.SpecValue> values = new ArrayList<>();
            for (String option : optionSet) {
                GoodsDetailResponse.SpecValue sv = new GoodsDetailResponse.SpecValue();
                sv.setName(option);
                sv.setValueName(option);
                sv.setAvailable(availableValueKeys.contains(name + "::" + option));
                sv.setDesc(option);
                sv.setPicture("");
                values.add(sv);
            }
            item.setValues(values);
            items.add(item);
        }
        return items;
    }
}
