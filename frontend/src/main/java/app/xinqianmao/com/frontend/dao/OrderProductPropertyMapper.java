/**
 * File: OrderProductPropertyMapper.java
 * Author: system
 * Date: 2026-05-11
 */
package app.xinqianmao.com.frontend.dao;

import app.xinqianmao.com.frontend.common.entity.OrderProductProperty;
import app.xinqianmao.com.common.dao.TenantBaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderProductPropertyMapper extends TenantBaseMapper<OrderProductProperty> {
}
