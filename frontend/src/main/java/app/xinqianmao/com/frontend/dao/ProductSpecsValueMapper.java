/**
 * File: ProductSpecsValueMapper.java
 * Author: system
 * Date: 2026-05-16
 */
package app.xinqianmao.com.frontend.dao;

import app.xinqianmao.com.common.dao.TenantBaseMapper;
import app.xinqianmao.com.frontend.common.entity.ProductSpecsValue;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductSpecsValueMapper extends TenantBaseMapper<ProductSpecsValue> {
}
