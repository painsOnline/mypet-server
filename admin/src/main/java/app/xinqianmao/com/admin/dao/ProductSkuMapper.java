/**
 * File: ProductSkuMapper.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.dao;

import app.xinqianmao.com.admin.common.entity.ProductSku;
import app.xinqianmao.com.common.dao.TenantBaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductSkuMapper extends TenantBaseMapper<ProductSku> {
}
