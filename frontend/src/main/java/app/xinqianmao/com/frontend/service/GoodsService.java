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
import app.xinqianmao.com.frontend.dao.ProductSpecsValueMapper;
import app.xinqianmao.com.frontend.common.entity.ProductSpecsValue;
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
    private final ProductSpecsValueMapper specsValueMapper;

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
                .collect(Collectors.toMap(s -> s.getId(), ProductSpecs::getName, (a, b) -> a));

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
            si.setSpecs(parseSpecsJson(sku.getSpecs(), specsNameMap));
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
    private List<GoodsDetailResponse.SpecValue> parseSpecsJson(String specsJson, Map<String, String> specIdToName) {
        if (specsJson == null || specsJson.isBlank()) return List.of();
        try {
            List<Map<String, String>> list = OBJECT_MAPPER.readValue(specsJson,
                    new TypeReference<List<Map<String, String>>>() {});
            return list.stream().map(m -> {
                GoodsDetailResponse.SpecValue sv = new GoodsDetailResponse.SpecValue();
                String sid = m.containsKey("spec_id") ? m.get("spec_id") : m.get("specId");
                sv.setSpecId(sid);
                sv.setSpecName(specIdToName != null && sid != null ? specIdToName.get(sid) : null);
                sv.setValueName(m.containsKey("value_name") ? m.get("value_name") : m.get("valueName"));
                sv.setValueId(m.containsKey("value_id") ? m.get("value_id") : m.get("valueId"));
                return sv;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            return List.of();
        }
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
        loadSpecValues(specDefs);

        Map<String, String> specIdToName = new HashMap<>();
        for (ProductSpecs s : specDefs) specIdToName.put(s.getId() != null ? s.getId() : "", s.getName());

        Set<String> availableValueKeys = new HashSet<>();
        Map<String, Map<String, GoodsDetailResponse.SpecValue>> skuValueMap = new HashMap<>();
        for (ProductSku sku : skus) {
            List<GoodsDetailResponse.SpecValue> svList = parseSpecsJson(sku.getSpecs(), null);
            for (GoodsDetailResponse.SpecValue sv : svList) {
                String specName = specIdToName.get(sv.getSpecId());
                if (specName == null) specName = sv.getSpecName();
                if (specName != null && sv.getValueName() != null) {
                    sv.setSpecName(specName);
                    availableValueKeys.add(specName + "::" + sv.getValueName());
                    skuValueMap.computeIfAbsent(specName, k -> new LinkedHashMap<>())
                            .put(sv.getValueName(), sv);
                }
            }
        }

        List<GoodsDetailResponse.SpecItem> items = new ArrayList<>();
        for (ProductSpecs spec : specDefs) {
            if (spec.getType() == null || spec.getType() != 1) continue;

            GoodsDetailResponse.SpecItem item = new GoodsDetailResponse.SpecItem();
            item.setSpecName(spec.getName());
            item.setSpecId(spec.getId());
            item.setSort(spec.getSort());
            item.setInputType(spec.getInputType());

            // Collect unique value names from spec definition AND actual SKU values
            Map<String, String> valueNameToId = new LinkedHashMap<>();
            List<ProductSpecsValue> defValues = spec.getValuesList();
            if (defValues != null) {
                for (ProductSpecsValue psv : defValues) {
                    if (psv.getValueName() != null) {
                        valueNameToId.putIfAbsent(psv.getValueName(), psv.getId());
                    }
                }
            }
            // Also include values from actual SKU specs for this spec name
            Map<String, GoodsDetailResponse.SpecValue> extraValues = skuValueMap.get(spec.getName());
            if (extraValues != null) {
                for (Map.Entry<String, GoodsDetailResponse.SpecValue> e : extraValues.entrySet()) {
                    if (e.getValue().getValueId() != null) {
                        valueNameToId.putIfAbsent(e.getKey(), e.getValue().getValueId());
                    } else {
                        valueNameToId.putIfAbsent(e.getKey(), null);
                    }
                }
            }

            List<GoodsDetailResponse.SpecValue> values = new ArrayList<>();
            for (Map.Entry<String, String> entry : valueNameToId.entrySet()) {
                String valueName = entry.getKey();
                String valueId = entry.getValue();
                GoodsDetailResponse.SpecValue sv = new GoodsDetailResponse.SpecValue();
                sv.setValueName(valueName);
                sv.setValueId(valueId);
                sv.setAvailable(availableValueKeys.contains(spec.getName() + "::" + valueName));
                values.add(sv);
            }
            item.setValues(values);
            items.add(item);
        }
        return items;
    }
}
