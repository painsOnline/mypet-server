/**
 * File: ProductSpecsMapper.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.dao;

import app.xinqianmao.com.admin.common.entity.ProductSpecs;
import app.xinqianmao.com.common.dao.TenantBaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ProductSpecsMapper extends TenantBaseMapper<ProductSpecs> {

    List<ProductSpecs> selectByTypeId(String productType);
}
