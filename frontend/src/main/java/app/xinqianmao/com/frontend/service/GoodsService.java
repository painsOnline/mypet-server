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

        // Load value_id -> value_name map for non-unique specs
        Map<String, String> valueIdToName = new HashMap<>();
        java.util.Set<String> vids = properties.stream().map(ProductProperty::getValueId)
            .filter(v -> v != null && !v.isBlank()).collect(Collectors.toSet());
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
                String sid = m.get("spec_id");
                sv.setSpecId(sid);
                String sname = specIdToName != null && sid != null ? specIdToName.get(sid) : null;
                if (sname == null) sname = m.get("spec_name");
                sv.setSpecName(sname);
                sv.setValueName(m.get("value_name"));
                sv.setValueId(m.get("value_id"));
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
        Map<String, String> specIdToName = new HashMap<>();
        for (ProductSpecs s : specDefs) if (s.getId() != null) specIdToName.put(s.getId(), s.getName());
        Map<String, GoodsDetailResponse.SpecItem> specMap = new LinkedHashMap<>();
        Map<String, Map<String, GoodsDetailResponse.SpecValue>> specValues = new LinkedHashMap<>();
        for (ProductSku sku : skus) {
            List<GoodsDetailResponse.SpecValue> svList = parseSpecsJson(sku.getSpecs(), specIdToName);
            for (GoodsDetailResponse.SpecValue sv : svList) {
                if (sv.getSpecId() == null) continue;
                final String sn = specIdToName.getOrDefault(sv.getSpecId(),
                    sv.getSpecName() != null ? sv.getSpecName() : "");
                specMap.computeIfAbsent(sv.getSpecId(), k -> {
                    GoodsDetailResponse.SpecItem si = new GoodsDetailResponse.SpecItem();
                    si.setSpecId(sv.getSpecId());
                    si.setSpecName(sn);
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
