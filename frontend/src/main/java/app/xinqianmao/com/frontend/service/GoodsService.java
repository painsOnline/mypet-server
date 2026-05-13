/**
 * File: GoodsService.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.frontend.service;

import app.xinqianmao.com.common.exception.BizException;
import app.xinqianmao.com.frontend.common.entity.*;
import app.xinqianmao.com.frontend.common.pojo.GoodsDetailResponse;
import app.xinqianmao.com.frontend.dao.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Product/goods detail service for mini-program frontend.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsService {

    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final ProductPropertyMapper productPropertyMapper;
    private final ProductSpecsMapper productSpecsMapper;
    private final ProductTypeSpecRelMapper productTypeSpecRelMapper;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Get full product detail: product info + properties + SKUs + specs for SKU selection UI.
     */
    public GoodsDetailResponse getDetail(String productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BizException("404", "商品不存在");
        }

        // Properties
        List<ProductProperty> properties = productPropertyMapper.selectList(
                new LambdaQueryWrapper<ProductProperty>()
                        .eq(ProductProperty::getProductId, productId)
                        .eq(ProductProperty::getIsDelete, 0)
                        .orderByAsc(ProductProperty::getSort));

        // SKUs
        List<ProductSku> skus = productSkuMapper.selectList(
                new LambdaQueryWrapper<ProductSku>()
                        .eq(ProductSku::getProductId, productId)
                        .eq(ProductSku::getIsDelete, 0));

        // Specs (from type-spec relation + global specs)
        List<ProductTypeSpecRel> rels = productTypeSpecRelMapper.selectList(
                new LambdaQueryWrapper<ProductTypeSpecRel>()
                        .eq(ProductTypeSpecRel::getProductType, product.getProductType()));
        List<String> relatedSpecIds = rels.stream().map(ProductTypeSpecRel::getSpecsId).collect(Collectors.toList());
        List<ProductSpecs> globalSpecs = productSpecsMapper.selectList(
                new LambdaQueryWrapper<ProductSpecs>()
                        .eq(ProductSpecs::getScope, 0)
                        .orderByAsc(ProductSpecs::getSort));
        List<ProductSpecs> specDefs = new ArrayList<>(globalSpecs);
        if (!relatedSpecIds.isEmpty()) {
            specDefs.addAll(productSpecsMapper.selectBatchIds(relatedSpecIds));
        }

        return buildGoodsDetail(product, skus, properties, specDefs);
    }

    // --- Helper methods ---

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

        // Details block
        GoodsDetailResponse.DetailInfo details = new GoodsDetailResponse.DetailInfo();
        details.setProperties(properties.stream().map(prop -> {
            GoodsDetailResponse.PropertyItem pi = new GoodsDetailResponse.PropertyItem();
            pi.setName(specsNameMap.getOrDefault(prop.getSpecsId(), ""));
            pi.setValue(prop.getValueName());
            return pi;
        }).collect(Collectors.toList()));
        details.setPictures(parseDetailPictures(product.getDetail()));
        r.setDetails(details);

        // SKU list
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

        // Specs for SKU selection UI
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
