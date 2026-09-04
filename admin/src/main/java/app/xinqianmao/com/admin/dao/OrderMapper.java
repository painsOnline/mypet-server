/**
 * File: OrderMapper.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.dao;

import app.xinqianmao.com.admin.common.entity.Order;
import app.xinqianmao.com.common.dao.TenantBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper extends TenantBaseMapper<Order> {

    /** Select orders with status 1 (pending delivery) */
    @Select("SELECT * FROM t_order WHERE order_status = 1 ORDER BY modify_time DESC NULLS LAST")
    List<Order> selectPendingOrders();

    /**
     * Auto-complete orders: status 3 (received) → 4 (completed),
     * for orders paid more than 7 days ago.
     */
    @Update("UPDATE t_order SET order_status = 4, finish_time = NOW(), modify_time = NOW() " +
            "WHERE order_status = 3 AND pay_time IS NOT NULL AND pay_time < #{cutoff}")
    int updateStatusToCompleted(@Param("cutoff") LocalDateTime cutoff);
}
