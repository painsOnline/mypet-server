/**
 * File: ProductTypeService.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.service;

import app.xinqianmao.com.admin.common.entity.ProductType;
import app.xinqianmao.com.admin.common.entity.ProductSpecs;
import app.xinqianmao.com.admin.common.pojo.TypeSaveRequest;
import app.xinqianmao.com.admin.common.pojo.TypeWithSpecsResponse;
import app.xinqianmao.com.admin.dao.ProductTypeMapper;
import app.xinqianmao.com.admin.dao.ProductSpecsMapper;
import app.xinqianmao.com.admin.dao.ProductMapper;
import app.xinqianmao.com.admin.dao.ProductSkuMapper;
import app.xinqianmao.com.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductTypeService {

    private final ProductTypeMapper typeMapper;
    private final ProductSpecsMapper specsMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper skuMapper;

    public List<TypeWithSpecsResponse> listAllWithSpecs() {
        List<ProductType> types = typeMapper.selectList(
                new LambdaQueryWrapper<ProductType>().orderByAsc(ProductType::getSort));
        if (types.isEmpty()) return List.of();

        List<String> typeIds = types.stream().map(ProductType::getId).toList();
        List<ProductSpecs> allSpecs = specsMapper.selectList(
                new LambdaQueryWrapper<ProductSpecs>().in(ProductSpecs::getProductType, typeIds)
                        .orderByAsc(ProductSpecs::getSort));

        Map<String, List<ProductSpecs>> specsByType = new HashMap<>();
        for (ProductSpecs spec : allSpecs) {
            specsByType.computeIfAbsent(spec.getProductType(), k -> new ArrayList<>()).add(spec);
        }

        return types.stream()
                .map(t -> {
                    long count = productMapper.countByTypeId(t.getId());
                    List<ProductSpecs> specs = specsByType.getOrDefault(t.getId(), List.of());

                    // Populate usedOptions for SKU specs that have products
                    if (count > 0) {
                        // Build map: specName → set of used valueNames from existing SKUs
                        List<Map<String, Object>> usedRows = skuMapper.findUsedSpecValuesByType(t.getId());
                        Map<String, Set<String>> usedBySpec = new HashMap<>();
                        for (Map<String, Object> row : usedRows) {
                            String specName = (String) row.get("specname");
                            String valueName = (String) row.get("valuename");
                            if (specName != null && valueName != null) {
                                usedBySpec.computeIfAbsent(specName, k -> new HashSet<>()).add(valueName);
                            }
                        }
                        // Set usedOptions on each SKU spec
                        for (ProductSpecs spec : specs) {
                            if (spec.getType() == 1) {
                                Set<String> used = usedBySpec.get(spec.getName());
                                spec.setUsedOptions(used != null ? new ArrayList<>(used) : List.of());
                            }
                        }
                    }

                    return TypeWithSpecsResponse.from(t, specs, count);
                })
                .toList();
    }

    public List<ProductType> listAll() {
        return typeMapper.selectList(
                new LambdaQueryWrapper<ProductType>().orderByAsc(ProductType::getSort));
    }

    public ProductType create(TypeSaveRequest req) {
        ProductType type = new ProductType();
        type.setName(req.getName());
        type.setSort(req.getSort() != null ? req.getSort() : 0);
        type.setCreateTime(LocalDateTime.now(app.xinqianmao.com.common.utils.DateTimeUtil.ZONE_BEIJING));
        typeMapper.insert(type);

        // Auto-create default SKU spec
        ProductSpecs defaultSpec = new ProductSpecs();
        defaultSpec.setProductType(type.getId());
        defaultSpec.setName("默认规格");
        defaultSpec.setType(1);  // SKU
        defaultSpec.setInputType(1);  // Unique
        defaultSpec.setInputOptions(List.of("默认规格"));
        defaultSpec.setSort(0);
        defaultSpec.setCreateTime(LocalDateTime.now(app.xinqianmao.com.common.utils.DateTimeUtil.ZONE_BEIJING));
        specsMapper.insert(defaultSpec);

        return type;
    }

    public void update(String id, TypeSaveRequest req) {
        ProductType type = typeMapper.selectById(id);
        if (type == null) throw new BizException("404", "类型不存在");
        type.setName(req.getName());
        type.setSort(req.getSort() != null ? req.getSort() : 0);
        type.setModifyTime(LocalDateTime.now(app.xinqianmao.com.common.utils.DateTimeUtil.ZONE_BEIJING));
        typeMapper.updateById(type);
    }

    public void delete(String id) {
        long count = productMapper.countByTypeId(id);
        if (count > 0) {
            throw new BizException("400", "该类型下有" + count + "个商品，无法删除");
        }
        specsMapper.delete(new LambdaQueryWrapper<ProductSpecs>().eq(ProductSpecs::getProductType, id));
        typeMapper.deleteById(id);
    }

    public List<ProductSpecs> getSpecs(String typeId) {
        return specsMapper.selectList(
                new LambdaQueryWrapper<ProductSpecs>()
                        .eq(ProductSpecs::getProductType, typeId)
                        .orderByAsc(ProductSpecs::getSort));
    }
}
