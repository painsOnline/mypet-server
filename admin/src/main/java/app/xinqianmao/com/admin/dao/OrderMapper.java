/**
 * File: OrderMapper.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.dao;

import app.xinqianmao.com.admin.common.entity.Order;
import app.xinqianmao.com.common.dao.TenantBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderMapper extends TenantBaseMapper<Order> {

    /** Select orders with status 1 (pending delivery) or 3 with pending refund (no specific flag, just status 3) */
    @Select("SELECT * FROM t_order WHERE order_status = 1 ORDER BY modify_time DESC NULLS LAST")
    List<Order> selectPendingOrders();
}
