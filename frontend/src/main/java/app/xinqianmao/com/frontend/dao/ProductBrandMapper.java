/**
 * File: ProductBrandMapper.java
 * Author: system
 * Date: 2026-05-11
 */
package app.xinqianmao.com.frontend.dao;

import app.xinqianmao.com.frontend.common.entity.ProductBrand;
import app.xinqianmao.com.common.dao.TenantBaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductBrandMapper extends TenantBaseMapper<ProductBrand> {
}
