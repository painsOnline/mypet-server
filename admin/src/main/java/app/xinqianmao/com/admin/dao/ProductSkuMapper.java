/**
 * File: ProductSkuMapper.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.dao;

import app.xinqianmao.com.admin.common.entity.ProductSku;
import app.xinqianmao.com.common.dao.TenantBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProductSkuMapper extends TenantBaseMapper<ProductSku> {

    /**
     * Find all (specName, valueName) pairs used by SKUs under a given product type.
     * Used to determine which option values are already in use and cannot be deleted.
     */
    @Select("SELECT DISTINCT COALESCE(elem ->> 'spec_name', elem ->> 'name') AS specName, " +
            "COALESCE(elem ->> 'value_name', elem ->> 'valueName') AS valueName " +
            "FROM t_product_sku sku " +
            "JOIN t_product p ON p.id = sku.product_id " +
            "CROSS JOIN jsonb_array_elements(sku.specs::jsonb) AS elem " +
            "WHERE p.product_type = #{typeId} AND sku.is_delete = 0")
    List<Map<String, Object>> findUsedSpecValuesByType(@Param("typeId") String typeId);

    /** Count SKUs whose specs JSONB contains the given value_id. */
    @Select("SELECT COUNT(*) FROM t_product_sku WHERE is_delete = 0 AND specs::jsonb @> ('[{\"value_id\":\"' || #{valueId} || '\"}]')::jsonb")
    int countByValueId(@Param("valueId") String valueId);
}
