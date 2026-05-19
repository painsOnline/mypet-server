/**
 * File: SpecsService.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.service;

import app.xinqianmao.com.admin.common.entity.ProductSpecs;
import app.xinqianmao.com.admin.common.entity.ProductType;
import app.xinqianmao.com.admin.common.entity.ProductTypeSpecRel;
import app.xinqianmao.com.admin.common.pojo.SpecsSaveRequest;
import app.xinqianmao.com.admin.common.entity.ProductSpecsValue;
import app.xinqianmao.com.admin.dao.ProductSpecsMapper;
import app.xinqianmao.com.admin.dao.ProductSpecsValueMapper;
import app.xinqianmao.com.admin.dao.ProductTypeSpecRelMapper;
import app.xinqianmao.com.admin.common.entity.ProductProperty;
import app.xinqianmao.com.admin.dao.ProductMapper;
import app.xinqianmao.com.admin.dao.ProductPropertyMapper;
import app.xinqianmao.com.admin.dao.ProductSkuMapper;
import app.xinqianmao.com.admin.dao.OrderProductSkuMapper;
import app.xinqianmao.com.admin.dao.ProductTypeMapper;
import app.xinqianmao.com.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Specs management under a product type.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpecsService {

    private final ProductSpecsMapper specsMapper;
    private final ProductSpecsValueMapper specsValueMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final OrderProductSkuMapper orderProductSkuMapper;
    private final ProductTypeSpecRelMapper typeSpecRelMapper;
    private final ProductTypeMapper typeMapper;
    private final ProductPropertyMapper propertyMapper;

    public List<ProductSpecs> listByScope(Integer scope) {
        LambdaQueryWrapper<ProductSpecs> wrapper = new LambdaQueryWrapper<>();
        if (scope != null) {
            wrapper.eq(ProductSpecs::getScope, scope);
        }
        wrapper.orderByAsc(ProductSpecs::getSort);
        List<ProductSpecs> specs = specsMapper.selectList(wrapper);
        loadSpecValues(specs);
        if (!specs.isEmpty()) {
            // Load linked type names
            List<String> specIds = specs.stream().map(ProductSpecs::getId).collect(Collectors.toList());
            // Build spec-id -> list of type names
            Map<String, List<String>> typeNamesBySpecId = new HashMap<>();
            List<ProductTypeSpecRel> allRels = typeSpecRelMapper.selectList(
                    new LambdaQueryWrapper<ProductTypeSpecRel>().in(ProductTypeSpecRel::getSpecsId, specIds));
            if (!allRels.isEmpty()) {
                Set<String> typeIds = allRels.stream().map(ProductTypeSpecRel::getProductType).collect(Collectors.toSet());
                List<ProductType> types = typeMapper.selectBatchIds(typeIds).stream()
                        .filter(t -> t.getIsDelete() == null || t.getIsDelete() == 0)
                        .collect(Collectors.toList());
                Map<String, String> typeIdToName = new HashMap<>();
                types.forEach(t -> typeIdToName.put(t.getId(), t.getName()));
                Map<String, List<String>> result = new HashMap<>();
                for (ProductTypeSpecRel rel : allRels) {
                    String name = typeIdToName.get(rel.getProductType());
                    if (name != null) {
                        result.computeIfAbsent(rel.getSpecsId(), k -> new ArrayList<>()).add(name);
                    }
                }
                typeNamesBySpecId = result;
            }
            // Set linkedTypeNames on each spec
            for (ProductSpecs s : specs) {
                if (s.getScope() != null && s.getScope() == 0) {
                    s.setLinkedTypeNames(List.of("所有类型"));
                } else {
                    s.setLinkedTypeNames(typeNamesBySpecId.getOrDefault(s.getId(), List.of()));
                }
            }
        }
        return specs;
    }

    public List<ProductSpecs> listByType(String typeId) {
        // Global specs + type-linked specs via rel table
        List<ProductSpecs> global = specsMapper.selectList(
                new LambdaQueryWrapper<ProductSpecs>()
                        .eq(ProductSpecs::getScope, 0)
                        .orderByAsc(ProductSpecs::getSort));
        List<ProductTypeSpecRel> rels = typeSpecRelMapper.selectList(
                new LambdaQueryWrapper<ProductTypeSpecRel>()
                        .eq(ProductTypeSpecRel::getProductType, typeId));
        if (!rels.isEmpty()) {
            List<String> ids = rels.stream().map(ProductTypeSpecRel::getSpecsId).toList();
            List<ProductSpecs> typeSpecs = specsMapper.selectBatchIds(ids);
            // Sort type-linked specs by their sort field
            typeSpecs.sort(Comparator.comparingInt(s -> s.getSort() != null ? s.getSort() : 0));
            global.addAll(typeSpecs);
        }
        loadSpecValues(global);
        return global;
    }

    public void add(String typeId, SpecsSaveRequest req) {
        if (req.getInputOptions() == null || req.getInputOptions().isEmpty()) {
            throw new BizException("400", "规格值至少需要一个");
        }
        ProductSpecs spec = new ProductSpecs();
        spec.setName(req.getName());
        spec.setType(req.getType());
        spec.setInputType(req.getInputType());
        spec.setDesc(req.getDesc());
        // Use scope from request if provided, otherwise default to private
        spec.setScope(req.getScope() != null ? req.getScope() : 2);
        spec.setSort(req.getSort() != null ? req.getSort() : 0);
        spec.setCreateTime(LocalDateTime.now(app.xinqianmao.com.common.utils.DateTimeUtil.ZONE_BEIJING));
        specsMapper.insert(spec);

        // Save values to t_product_specs_value
        saveSpecValues(spec.getId(), req.getInputOptions());

        // Link to type only when typeId is provided (type page adds private specs)
        if (typeId != null && !typeId.isBlank()) {
            ProductTypeSpecRel rel = new ProductTypeSpecRel();
            rel.setId(UUID.randomUUID().toString());
            rel.setProductType(typeId);
            rel.setSpecsId(spec.getId());
            rel.setSort(spec.getSort());
            rel.setCreateTime(LocalDateTime.now(app.xinqianmao.com.common.utils.DateTimeUtil.ZONE_BEIJING));
            typeSpecRelMapper.insert(rel);
        }
    }

    public void update(String specId, SpecsSaveRequest req) {
        ProductSpecs spec = specsMapper.selectById(specId);
        if (spec == null) throw new BizException("404", "规格不存在");

        // Prevent changing the last SKU spec's type away from SKU
        if (spec.getType() == 1 && (req.getType() == null || req.getType() != 1)) {
            long skuCount = countSkuSpecs();
            if (skuCount <= 1) {
                throw new BizException("400", "至少需要一个SKU规格，无法修改该规格的类型");
            }
        }

        if (req.getName() != null) spec.setName(req.getName());
        if (req.getType() != null) spec.setType(req.getType());
        if (req.getInputType() != null) spec.setInputType(req.getInputType());
        if (req.getDesc() != null) spec.setDesc(req.getDesc());
        if (req.getSort() != null) spec.setSort(req.getSort());
        spec.setModifyTime(LocalDateTime.now(app.xinqianmao.com.common.utils.DateTimeUtil.ZONE_BEIJING));
        specsMapper.updateById(spec);
        // Values are managed separately via renameValue / deleteValue / addValue endpoints
    }

    public void delete(String specId) {
        ProductSpecs spec = specsMapper.selectById(specId);
        if (spec == null) throw new BizException("404", "规格不存在");

        // Shared specs cannot be deleted if still linked to any type — use unlink instead
        if (spec.getScope() != null && spec.getScope() == 1) {
            long relCount = typeSpecRelMapper.selectCount(
                    new LambdaQueryWrapper<ProductTypeSpecRel>()
                            .eq(ProductTypeSpecRel::getSpecsId, specId));
            if (relCount > 0) {
                throw new BizException("400", "该共享属性已被商品类型引用，请先在对应类型中解除引用");
            }
        }

        // Prevent deleting if any product is using this spec
        if (spec.getScope() != null && spec.getScope() != 0) {
            long productCount = propertyMapper.selectCount(
                    new LambdaQueryWrapper<ProductProperty>()
                            .eq(ProductProperty::getSpecsId, specId)
                            .eq(ProductProperty::getIsDelete, 0));
            if (productCount > 0) {
                throw new BizException("400", "该属性已被商品使用，无法删除");
            }
        }

        // Prevent deleting the last SKU spec
        if (spec.getType() == 1) {
            long skuCount = countSkuSpecs();
            if (skuCount <= 1) {
                throw new BizException("400", "至少需要一个SKU规格，无法删除该规格");
            }
            // Prevent deleting SKU spec if any linked type already has products
            List<ProductTypeSpecRel> rels = typeSpecRelMapper.selectList(
                    new LambdaQueryWrapper<ProductTypeSpecRel>()
                            .eq(ProductTypeSpecRel::getSpecsId, specId));
            for (ProductTypeSpecRel rel : rels) {
                long productCount = productMapper.countByTypeId(rel.getProductType());
                if (productCount > 0) {
                    throw new BizException("400", "该规格关联的类型下已有商品，无法删除SKU规格");
                }
            }
        }

        // Clean up rels and values before deleting the spec itself
        typeSpecRelMapper.delete(new LambdaQueryWrapper<ProductTypeSpecRel>()
                .eq(ProductTypeSpecRel::getSpecsId, specId));
        specsValueMapper.delete(new LambdaQueryWrapper<ProductSpecsValue>()
                .eq(ProductSpecsValue::getSpecsId, specId));
        specsMapper.deleteById(specId);
    }

    /**
     * Update only the values of a spec.
     */
    public void updateValues(String specId, List<String> inputOptions) {
        if (inputOptions == null || inputOptions.isEmpty()) {
            throw new BizException("400", "规格值至少需要一个");
        }
        ProductSpecs spec = specsMapper.selectById(specId);
        if (spec == null) throw new BizException("404", "规格不存在");
        spec.setModifyTime(LocalDateTime.now(app.xinqianmao.com.common.utils.DateTimeUtil.ZONE_BEIJING));
        specsMapper.updateById(spec);
        specsValueMapper.delete(new LambdaQueryWrapper<ProductSpecsValue>()
                .eq(ProductSpecsValue::getSpecsId, specId));
        saveSpecValues(specId, inputOptions);
    }

    /**
     * Rename a single spec value in-place (keeps ID, updates value_name).
     */
    public void renameValue(String valueId, String newValueName) {
        if (newValueName == null || newValueName.isBlank()) throw new BizException("400", "规格值名称不能为空");
        ProductSpecsValue psv = specsValueMapper.selectById(valueId);
        if (psv == null) throw new BizException("404", "规格值不存在");
        psv.setValueName(newValueName.trim());
        psv.setModifyTime(LocalDateTime.now(app.xinqianmao.com.common.utils.DateTimeUtil.ZONE_BEIJING));
        specsValueMapper.updateById(psv);
    }

    /** Delete a single spec value by ID. Fails if the value is in use. */
    public void deleteValue(String valueId) {
        ProductSpecsValue psv = specsValueMapper.selectById(valueId);
        if (psv == null) throw new BizException("404", "规格值不存在");
        // Check if referenced by product properties
        long propCount = propertyMapper.selectCount(
                new LambdaQueryWrapper<ProductProperty>().eq(ProductProperty::getValueId, valueId));
        if (propCount > 0) throw new BizException("400", "该规格值已被商品属性使用，无法删除");
        // Check if referenced by product SKUs (JSONB)
        int skuCount = productSkuMapper.countByValueId(valueId);
        if (skuCount > 0) throw new BizException("400", "该规格值已被商品SKU使用，无法删除");
        // Check if referenced by order SKUs (JSONB snapshot)
        int orderSkuCount = orderProductSkuMapper.countByValueId(valueId);
        if (orderSkuCount > 0) throw new BizException("400", "该规格值已被订单SKU使用，无法删除");
        specsValueMapper.deleteById(valueId);
    }

    /** Add a single spec value and return its new ID. */
    public String addValue(String specId, String valueName) {
        if (valueName == null || valueName.isBlank()) throw new BizException("400", "规格值名称不能为空");
        ProductSpecs psv = specsMapper.selectById(specId);
        if (psv == null) throw new BizException("404", "规格不存在");
        ProductSpecsValue sv = new ProductSpecsValue();
        sv.setSpecsId(specId);
        sv.setValueName(valueName.trim());
        sv.setSort(999);
        sv.setCreateTime(LocalDateTime.now(app.xinqianmao.com.common.utils.DateTimeUtil.ZONE_BEIJING));
        specsValueMapper.insert(sv);
        return sv.getId();
    }

    private void saveSpecValues(String specsId, List<String> values) {
        for (int i = 0; i < values.size(); i++) {
            String v = values.get(i);
            if (v == null || v.isBlank()) continue;
            ProductSpecsValue psv = new ProductSpecsValue();
            psv.setSpecsId(specsId);
            psv.setValueName(v.trim());
            psv.setSort(i);
            psv.setCreateTime(LocalDateTime.now(app.xinqianmao.com.common.utils.DateTimeUtil.ZONE_BEIJING));
            specsValueMapper.insert(psv);
        }
    }

    void loadSpecValues(List<ProductSpecs> specs) {
        if (specs.isEmpty()) return;
        List<String> specIds = specs.stream().map(ProductSpecs::getId).collect(Collectors.toList());
        List<ProductSpecsValue> allValues = specsValueMapper.selectList(
                new LambdaQueryWrapper<ProductSpecsValue>().in(ProductSpecsValue::getSpecsId, specIds)
                        .orderByAsc(ProductSpecsValue::getSort));
        Map<String, List<ProductSpecsValue>> valuesMap = new HashMap<>();
        Map<String, List<String>> optionsMap = new HashMap<>();
        for (ProductSpecsValue v : allValues) {
            valuesMap.computeIfAbsent(v.getSpecsId(), k -> new ArrayList<>()).add(v);
            optionsMap.computeIfAbsent(v.getSpecsId(), k -> new ArrayList<>()).add(v.getValueName());
        }
        for (ProductSpecs s : specs) {
            s.setValuesList(valuesMap.getOrDefault(s.getId(), List.of()));
            s.setInputOptions(optionsMap.getOrDefault(s.getId(), List.of()));
        }
    }

    /** Remove the rel entry between a type and a shared spec (does NOT delete the spec). */
    public void unlinkSpecFromType(String typeId, String specsId) {
        ProductTypeSpecRel rel = typeSpecRelMapper.selectOne(
                new LambdaQueryWrapper<ProductTypeSpecRel>()
                        .eq(ProductTypeSpecRel::getProductType, typeId)
                        .eq(ProductTypeSpecRel::getSpecsId, specsId));
        if (rel == null) throw new BizException("404", "关联不存在");
        typeSpecRelMapper.deleteById(rel.getId());
    }

    /** Link an existing shared spec to a type (only creates rel entry). */
    public void linkSpecToType(String typeId, String specsId) {
        // Prevent duplicate rels
        if (typeSpecRelMapper.selectCount(
                new LambdaQueryWrapper<ProductTypeSpecRel>()
                        .eq(ProductTypeSpecRel::getProductType, typeId)
                        .eq(ProductTypeSpecRel::getSpecsId, specsId)) > 0) {
            throw new BizException("400", "该属性已被该类型引用");
        }
        ProductTypeSpecRel rel = new ProductTypeSpecRel();
        rel.setId(UUID.randomUUID().toString());
        rel.setProductType(typeId);
        rel.setSpecsId(specsId);
        rel.setSort(0);
        rel.setCreateTime(LocalDateTime.now(app.xinqianmao.com.common.utils.DateTimeUtil.ZONE_BEIJING));
        typeSpecRelMapper.insert(rel);
    }

    private long countSkuSpecs() {
        return specsMapper.selectCount(
                new LambdaQueryWrapper<ProductSpecs>()
                        .eq(ProductSpecs::getType, 1));
    }
}
