/**
 * File: ConfigTenantMapper.java
 * Author: system
 * Date: 2026-05-21
 */
package app.xinqianmao.com.tenant.dao;

import app.xinqianmao.com.common.entity.Tenant;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ConfigTenantMapper extends BaseMapper<Tenant> {
}
