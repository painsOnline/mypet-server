/**
 * File: TenantMapper.java
 * Author: system
 * Date: 2026-05-08
 */
package app.xinqianmao.com.common.dao;

import app.xinqianmao.com.common.entity.Tenant;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * Mapper for c_tenant in mypet_config.
 * Only usable when DataSource is routed to config DB.
 */
public interface TenantMapper extends BaseMapper<Tenant> {
}
