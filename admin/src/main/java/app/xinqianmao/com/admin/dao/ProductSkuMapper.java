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
    @Select("SELECT DISTINCT elem ->> 'name' AS specName, elem ->> 'valueName' AS valueName " +
            "FROM t_product_sku, json_array_elements(specs) AS elem " +
            "WHERE product_type = #{typeId}")
    List<Map<String, Object>> findUsedSpecValuesByType(@Param("typeId") String typeId);
}
