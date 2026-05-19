/**
 * File: OrderProductSkuMapper.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.dao;

import app.xinqianmao.com.admin.common.entity.OrderProductSku;
import app.xinqianmao.com.common.dao.TenantBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrderProductSkuMapper extends TenantBaseMapper<OrderProductSku> {

    /** Count order SKUs whose specs JSONB contains the given value_id. */
    @Select("SELECT COUNT(*) FROM t_order_product_skus WHERE is_delete = 0 AND specs::jsonb @> ('[{\"value_id\":\"' || #{valueId} || '\"}]')::jsonb")
    int countByValueId(@Param("valueId") String valueId);
}
