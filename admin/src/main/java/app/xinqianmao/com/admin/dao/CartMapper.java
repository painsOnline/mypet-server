/**
 * File: CartMapper.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.dao;

import app.xinqianmao.com.admin.common.entity.Cart;
import app.xinqianmao.com.common.dao.TenantBaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CartMapper extends TenantBaseMapper<Cart> {
}
