/**
 * File: OrderProductSkuMapper.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.frontend.dao;

import app.xinqianmao.com.common.dao.TenantBaseMapper;
import app.xinqianmao.com.frontend.common.entity.OrderProductSku;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderProductSkuMapper extends TenantBaseMapper<OrderProductSku> {
}
