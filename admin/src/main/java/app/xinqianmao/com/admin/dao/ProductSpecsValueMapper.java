/**
 * File: ProductSpecsValueMapper.java
 * Author: system
 * Date: 2026-05-16
 */
package app.xinqianmao.com.admin.dao;

import app.xinqianmao.com.admin.common.entity.ProductSpecsValue;
import app.xinqianmao.com.common.dao.TenantBaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductSpecsValueMapper extends TenantBaseMapper<ProductSpecsValue> {
}
