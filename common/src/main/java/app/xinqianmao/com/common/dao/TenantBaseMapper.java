/**
 * File: BaseMapper.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.common.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * Base mapper for all tenant business DB tables.
 * Extends MyBatis-Plus BaseMapper to provide standard CRUD.
 */
public interface TenantBaseMapper<T> extends BaseMapper<T> {
}
