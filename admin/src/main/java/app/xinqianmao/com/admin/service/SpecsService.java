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
import app.xinqianmao.com.admin.dao.ProductSpecsMapper;
import app.xinqianmao.com.admin.dao.ProductTypeSpecRelMapper;
import app.xinqianmao.com.admin.dao.ProductMapper;
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
    private final ProductMapper productMapper;
    private final ProductTypeSpecRelMapper typeSpecRelMapper;
    private final ProductTypeMapper typeMapper;

    public List<ProductSpecs> listByScope(Integer scope) {
        LambdaQueryWrapper<ProductSpecs> wrapper = new LambdaQueryWrapper<>();
        if (scope != null) {
            wrapper.eq(ProductSpecs::getScope, scope);
        }
        wrapper.orderByAsc(ProductSpecs::getSort);
        List<ProductSpecs> specs = specsMapper.selectList(wrapper);
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
        spec.setInputOptions(req.getInputOptions());
        spec.setDesc(req.getDesc());
        // Use scope from request if provided, otherwise default to private
        spec.setScope(req.getScope() != null ? req.getScope() : 2);
        spec.setSort(req.getSort() != null ? req.getSort() : 0);
        spec.setCreateTime(LocalDateTime.now(app.xinqianmao.com.common.utils.DateTimeUtil.ZONE_BEIJING));
        specsMapper.insert(spec);

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
        if (req.getInputOptions() == null || req.getInputOptions().isEmpty()) {
            throw new BizException("400", "规格值至少需要一个");
        }
        ProductSpecs spec = specsMapper.selectById(specId);
        if (spec == null) throw new BizException("404", "规格不存在");

        // Prevent changing the last SKU spec's type away from SKU
        if (spec.getType() == 1 && (req.getType() == null || req.getType() != 1)) {
            long skuCount = countSkuSpecs();
            if (skuCount <= 1) {
                throw new BizException("400", "至少需要一个SKU规格，无法修改该规格的类型");
            }
        }

        spec.setName(req.getName());
        spec.setType(req.getType());
        spec.setInputType(req.getInputType());
        spec.setInputOptions(req.getInputOptions());
        spec.setDesc(req.getDesc());
        spec.setSort(req.getSort() != null ? req.getSort() : 0);
        spec.setModifyTime(LocalDateTime.now(app.xinqianmao.com.common.utils.DateTimeUtil.ZONE_BEIJING));
        specsMapper.updateById(spec);
    }

    public void delete(String specId) {
        ProductSpecs spec = specsMapper.selectById(specId);
        if (spec == null) throw new BizException("404", "规格不存在");

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

        specsMapper.deleteById(specId);
    }

    /**
     * Update only the inputOptions (values) of a spec.
     */
    public void updateValues(String specId, List<String> inputOptions) {
        if (inputOptions == null || inputOptions.isEmpty()) {
            throw new BizException("400", "规格值至少需要一个");
        }
        ProductSpecs spec = specsMapper.selectById(specId);
        if (spec == null) throw new BizException("404", "规格不存在");
        spec.setInputOptions(inputOptions);
        spec.setModifyTime(LocalDateTime.now(app.xinqianmao.com.common.utils.DateTimeUtil.ZONE_BEIJING));
        specsMapper.updateById(spec);
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
