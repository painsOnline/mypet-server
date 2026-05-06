/**
 * File: ProductSkuMapper.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.frontend.dao;

import app.xinqianmao.com.common.dao.TenantBaseMapper;
import app.xinqianmao.com.frontend.common.entity.ProductSku;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductSkuMapper extends TenantBaseMapper<ProductSku> {
}
