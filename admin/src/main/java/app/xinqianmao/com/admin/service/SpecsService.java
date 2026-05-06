/**
 * File: SpecsService.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.service;

import app.xinqianmao.com.admin.common.entity.ProductSpecs;
import app.xinqianmao.com.admin.common.pojo.SpecsSaveRequest;
import app.xinqianmao.com.admin.dao.ProductSpecsMapper;
import app.xinqianmao.com.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Specs management under a product type.
 */
@Service
@RequiredArgsConstructor
public class SpecsService {

    private final ProductSpecsMapper specsMapper;

    public List<ProductSpecs> listByType(String typeId) {
        return specsMapper.selectList(
                new LambdaQueryWrapper<ProductSpecs>()
                        .eq(ProductSpecs::getProductType, typeId)
                        .orderByAsc(ProductSpecs::getSort));
    }

    public void add(String typeId, SpecsSaveRequest req) {
        if (req.getInputOptions() == null || req.getInputOptions().isEmpty()) {
            throw new BizException("400", "规格值至少需要一个");
        }
        ProductSpecs spec = new ProductSpecs();
        spec.setProductType(typeId);
        spec.setName(req.getName());
        spec.setType(req.getType());
        spec.setInputType(req.getInputType());
        spec.setInputOptions(req.getInputOptions());
        spec.setDesc(req.getDesc());
        spec.setSort(req.getSort() != null ? req.getSort() : 0);
        spec.setCreateTime(LocalDateTime.now(app.xinqianmao.com.common.utils.DateTimeUtil.ZONE_BEIJING));
        specsMapper.insert(spec);
    }

    public void update(String specId, SpecsSaveRequest req) {
        if (req.getInputOptions() == null || req.getInputOptions().isEmpty()) {
            throw new BizException("400", "规格值至少需要一个");
        }
        ProductSpecs spec = specsMapper.selectById(specId);
        if (spec == null) throw new BizException("404", "规格不存在");

        // Prevent changing the last SKU spec's type away from SKU
        if (spec.getType() == 1 && (req.getType() == null || req.getType() != 1)) {
            long skuCount = countSkuSpecsByType(spec.getProductType());
            if (skuCount <= 1) {
                throw new BizException("400", "每个商品类型至少需要一个SKU规格，无法修改该默认规格的类型");
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
            long skuCount = countSkuSpecsByType(spec.getProductType());
            if (skuCount <= 1) {
                throw new BizException("400", "每个商品类型至少需要一个SKU规格，无法删除该默认规格");
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

    private long countSkuSpecsByType(String typeId) {
        return specsMapper.selectCount(
                new LambdaQueryWrapper<ProductSpecs>()
                        .eq(ProductSpecs::getProductType, typeId)
                        .eq(ProductSpecs::getType, 1));
    }
}
