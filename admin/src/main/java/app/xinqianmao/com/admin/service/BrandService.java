/**
 * File: BrandService.java
 * Author: system
 * Date: 2026-05-11
 */
package app.xinqianmao.com.admin.service;

import app.xinqianmao.com.admin.common.entity.ProductBrand;
import app.xinqianmao.com.admin.dao.ProductBrandMapper;
import app.xinqianmao.com.admin.dao.ProductMapper;
import app.xinqianmao.com.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final ProductBrandMapper brandMapper;
    private final ProductMapper productMapper;

    public List<ProductBrand> listAll() {
        return brandMapper.selectList(
                new LambdaQueryWrapper<ProductBrand>()
                        .eq(ProductBrand::getIsDelete, 0)
                        .orderByAsc(ProductBrand::getSort));
    }

    public ProductBrand create(ProductBrand brand) {
        brandMapper.insert(brand);
        return brand;
    }

    public ProductBrand update(ProductBrand brand) {
        ProductBrand existing = brandMapper.selectById(brand.getId());
        if (existing == null) throw new BizException("404", "品牌不存在");
        existing.setBrandName(brand.getBrandName());
        existing.setBrandEn(brand.getBrandEn());
        existing.setBrandLogo(brand.getBrandLogo());
        existing.setBrandDesc(brand.getBrandDesc());
        existing.setSort(brand.getSort());
        brandMapper.updateById(existing);
        return existing;
    }

    /** Soft delete - only if no products reference this brand */
    public void delete(String id) {
        ProductBrand brand = brandMapper.selectById(id);
        if (brand == null) throw new BizException("404", "品牌不存在");
        long count = productMapper.countByBrandId(id);
        if (count > 0) throw new BizException("400", "该品牌下有" + count + "个商品，无法删除");
        brand.setIsDelete(1);
        brandMapper.updateById(brand);
    }
}
