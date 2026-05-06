/**
 * File: CategoryService.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.service;

import app.xinqianmao.com.admin.common.entity.ProductCategory;
import app.xinqianmao.com.admin.common.pojo.CategoryListResponse;
import app.xinqianmao.com.admin.common.pojo.CategorySaveRequest;
import app.xinqianmao.com.admin.dao.ProductCategoryMapper;
import app.xinqianmao.com.admin.dao.ProductMapper;
import app.xinqianmao.com.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final ProductCategoryMapper categoryMapper;
    private final ProductMapper productMapper;

    public List<CategoryListResponse> listAll() {
        List<ProductCategory> cats = categoryMapper.selectList(
                new LambdaQueryWrapper<ProductCategory>().orderByAsc(ProductCategory::getSort));
        return cats.stream().map(cat -> {
            long count = productMapper.countByCategoryId(cat.getId());
            return CategoryListResponse.from(cat, count);
        }).toList();
    }

    public ProductCategory create(CategorySaveRequest req) {
        ProductCategory cat = new ProductCategory();
        cat.setName(req.getName());
        cat.setPicture(req.getPicture() != null ? req.getPicture() : "");
        cat.setSort(req.getSort() != null ? req.getSort() : 0);
        cat.setCreateTime(LocalDateTime.now(app.xinqianmao.com.common.utils.DateTimeUtil.ZONE_BEIJING));
        categoryMapper.insert(cat);
        return cat;
    }

    public void update(String id, CategorySaveRequest req) {
        ProductCategory cat = categoryMapper.selectById(id);
        if (cat == null) throw new BizException("404", "分类不存在");
        cat.setName(req.getName());
        cat.setPicture(req.getPicture() != null ? req.getPicture() : "");
        cat.setSort(req.getSort() != null ? req.getSort() : 0);
        cat.setModifyTime(LocalDateTime.now(app.xinqianmao.com.common.utils.DateTimeUtil.ZONE_BEIJING));
        categoryMapper.updateById(cat);
    }

    public void delete(String id) {
        long count = productMapper.countByCategoryId(id);
        if (count > 0) {
            throw new BizException("400", "该分类下有" + count + "个商品，无法删除");
        }
        categoryMapper.deleteById(id);
    }
}
