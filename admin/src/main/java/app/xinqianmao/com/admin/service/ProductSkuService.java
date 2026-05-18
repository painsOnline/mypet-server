/**
 * File: ProductSkuService.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.service;

import app.xinqianmao.com.admin.common.entity.ProductSku;
import app.xinqianmao.com.admin.common.entity.ProductSpecs;
import app.xinqianmao.com.admin.common.entity.ProductSpecsValue;
import app.xinqianmao.com.admin.common.entity.ProductTypeSpecRel;
import app.xinqianmao.com.admin.common.pojo.SkuSaveRequest;
import app.xinqianmao.com.admin.dao.ProductSkuMapper;
import app.xinqianmao.com.admin.dao.ProductSpecsMapper;
import app.xinqianmao.com.admin.dao.ProductSpecsValueMapper;
import app.xinqianmao.com.admin.dao.ProductTypeSpecRelMapper;
import app.xinqianmao.com.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SKU management and auto-generation from type specs.
 */
@Service
@RequiredArgsConstructor
public class ProductSkuService {

    private final ProductSkuMapper skuMapper;
    private final ProductSpecsMapper specsMapper;
    private final ProductSpecsValueMapper specsValueMapper;
    private final ProductTypeSpecRelMapper typeSpecRelMapper;

    /**
     * Generate SKU combinations from the product type's SKU-type specs.
     * Uses Cartesian product of all SKU-spec input_options.
     */
    @Transactional
    public void generate(String productId, String productType) {
        // Get SKU-type specs for this type via rel table
        List<ProductSpecs> specDefs = getSkuSpecsByType(productType);

        if (specDefs.isEmpty()) {
            throw new BizException("400", "该商品类型没有SKU规格定义");
        }

        // Soft-delete existing SKUs
        List<ProductSku> existing = skuMapper.selectList(
                new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getProductId, productId)
                        .eq(ProductSku::getIsDelete, 0));
        for (ProductSku s : existing) {
            s.setIsDelete(1);
            skuMapper.updateById(s);
        }

        // Generate Cartesian product of spec value combinations
        List<List<String>> allValues = specDefs.stream()
                .map(s -> s.getInputOptions() != null ? s.getInputOptions() : List.<String>of())
                .collect(Collectors.toList());

        List<List<String>> combinations = cartesianProduct(allValues);
        if (combinations.isEmpty()) {
            return;
        }

        ObjectMapper mapper = new ObjectMapper();
        for (List<String> combo : combinations) {
            // Build specs JSON
            List<Map<String, String>> specsList = new ArrayList<>();
            for (int i = 0; i < combo.size(); i++) {
                ProductSpecs specDef = specDefs.get(i);
                Map<String, String> m = new LinkedHashMap<>();
                m.put("spec_id", specDef.getId());
                m.put("spec_name", specDef.getName());
                String val = combo.get(i);
                m.put("value_name", val);
                // Find value_id from spec values
                String vid = "";
                if (specDef.getValuesList() != null) {
                    vid = specDef.getValuesList().stream()
                        .filter(v -> val.equals(v.getValueName()))
                        .findFirst().map(ProductSpecsValue::getId).orElse("");
                }
                m.put("value_id", vid);
                specsList.add(m);
            }

            ProductSku sku = new ProductSku();
            sku.setProductId(productId);
            sku.setPrice(java.math.BigDecimal.ZERO);
            sku.setOldPrice(java.math.BigDecimal.ZERO);
            sku.setInventory(0);
            sku.setPicture("");
            try {
                sku.setSpecs(mapper.writeValueAsString(specsList));
            } catch (JsonProcessingException e) {
                sku.setSpecs("[]");
            }
            sku.setCreateTime(LocalDateTime.now(app.xinqianmao.com.common.utils.DateTimeUtil.ZONE_BEIJING));
            skuMapper.insert(sku);
        }
    }

    /**
     * Add a single SKU.
     */
    public void add(String productId, String productType, SkuSaveRequest req) {
        ProductSku sku = new ProductSku();
        sku.setProductId(productId);
        sku.setPrice(req.getPrice());
        sku.setOldPrice(req.getOldPrice());
        sku.setInventory(req.getInventory() != null ? req.getInventory() : 0);
        sku.setPicture(req.getPicture() != null ? req.getPicture() : "");
        try {
            sku.setSpecs(new ObjectMapper().writeValueAsString(
                    req.getSpecs() != null ? req.getSpecs() : List.of()));
        } catch (JsonProcessingException e) {
            sku.setSpecs("[]");
        }
        sku.setCreateTime(LocalDateTime.now(app.xinqianmao.com.common.utils.DateTimeUtil.ZONE_BEIJING));
        skuMapper.insert(sku);
    }

    /**
     * Update SKU price, inventory, picture.
     */
    public void update(String skuId, SkuSaveRequest req) {
        ProductSku sku = skuMapper.selectById(skuId);
        if (sku == null) throw new BizException("404", "SKU不存在");
        sku.setPrice(req.getPrice());
        sku.setOldPrice(req.getOldPrice());
        sku.setInventory(req.getInventory() != null ? req.getInventory() : 0);
        sku.setPicture(req.getPicture() != null ? req.getPicture() : "");
        sku.setModifyTime(LocalDateTime.now(app.xinqianmao.com.common.utils.DateTimeUtil.ZONE_BEIJING));
        skuMapper.updateById(sku);
    }

    /**
     * Delete a SKU.
     */
    public void delete(String skuId) {
        skuMapper.deleteById(skuId);
    }

    /**
     * List all SKUs for a product.
     */
    public List<ProductSku> listByProduct(String productId) {
        return skuMapper.selectList(
                new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getProductId, productId)
                        .eq(ProductSku::getIsDelete, 0));
    }

    /**
     * Preview possible SKU spec combinations for a product type (read-only, no persistence).
     */
    public List<Map<String, String>> previewCombinations(String productType) {
        List<ProductSpecs> specDefs = getSkuSpecsByType(productType);

        if (specDefs.isEmpty()) {
            return List.of();
        }

        List<List<String>> allValues = specDefs.stream()
                .map(s -> s.getInputOptions() != null ? s.getInputOptions() : List.<String>of())
                .collect(Collectors.toList());

        List<List<String>> combinations = cartesianProduct(allValues);
        List<Map<String, String>> result = new ArrayList<>();
        for (List<String> combo : combinations) {
            Map<String, String> map = new LinkedHashMap<>();
            for (int i = 0; i < combo.size(); i++) {
                map.put(specDefs.get(i).getName(), combo.get(i));
            }
            result.add(map);
        }
        return result;
    }

    private List<ProductSpecs> getSkuSpecsByType(String productType) {
        List<ProductTypeSpecRel> rels = typeSpecRelMapper.selectList(
                new LambdaQueryWrapper<ProductTypeSpecRel>().eq(ProductTypeSpecRel::getProductType, productType));
        List<String> ids = rels.stream().map(ProductTypeSpecRel::getSpecsId).collect(Collectors.toList());
        // Also include global specs (scope=0)
        List<ProductSpecs> all = specsMapper.selectList(
                new LambdaQueryWrapper<ProductSpecs>().eq(ProductSpecs::getScope, 0).eq(ProductSpecs::getType, 1));
        if (!ids.isEmpty()) {
            List<ProductSpecs> typeSpecs = specsMapper.selectBatchIds(ids);
            all.addAll(typeSpecs);
        }
        // Load values from t_product_specs_value
        if (!all.isEmpty()) {
            List<String> specIds = all.stream().map(ProductSpecs::getId).collect(Collectors.toList());
            List<ProductSpecsValue> vals = specsValueMapper.selectList(
                    new LambdaQueryWrapper<ProductSpecsValue>().in(ProductSpecsValue::getSpecsId, specIds)
                            .orderByAsc(ProductSpecsValue::getSort));
            Map<String, List<String>> map = new HashMap<>();
            for (ProductSpecsValue v : vals) map.computeIfAbsent(v.getSpecsId(), k -> new ArrayList<>()).add(v.getValueName());
            for (ProductSpecs s : all) s.setInputOptions(map.getOrDefault(s.getId(), List.of()));
        }
        return all;
    }

    /**
     * Cartesian product of lists.
     */
    private List<List<String>> cartesianProduct(List<List<String>> lists) {
        if (lists.isEmpty()) return List.of();
        List<List<String>> result = new ArrayList<>();
        result.add(new ArrayList<>());
        for (List<String> values : lists) {
            if (values.isEmpty()) continue;
            List<List<String>> newResult = new ArrayList<>();
            for (List<String> current : result) {
                for (String value : values) {
                    List<String> copy = new ArrayList<>(current);
                    copy.add(value);
                    newResult.add(copy);
                }
            }
            result = newResult;
        }
        return result.size() == 1 && result.get(0).isEmpty() ? List.of() : result;
    }
}
