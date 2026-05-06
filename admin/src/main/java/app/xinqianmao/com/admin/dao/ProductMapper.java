/**
 * File: ProductMapper.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.dao;

import app.xinqianmao.com.admin.common.entity.Product;
import app.xinqianmao.com.common.dao.TenantBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProductMapper extends TenantBaseMapper<Product> {

    /** Count products by category (for delete check) */
    @Select("SELECT COUNT(*) FROM t_product WHERE product_category = #{categoryId}")
    Long countByCategoryId(String categoryId);

    /** Count products by type (for delete check) */
    @Select("SELECT COUNT(*) FROM t_product WHERE product_type = #{typeId}")
    Long countByTypeId(String typeId);
}
