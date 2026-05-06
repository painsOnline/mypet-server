/**
 * File: CartMapper.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.frontend.dao;

import app.xinqianmao.com.common.dao.TenantBaseMapper;
import app.xinqianmao.com.frontend.common.entity.Cart;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CartMapper extends TenantBaseMapper<Cart> {
}
