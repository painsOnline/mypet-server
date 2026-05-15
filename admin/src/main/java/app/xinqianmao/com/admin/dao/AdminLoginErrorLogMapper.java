package app.xinqianmao.com.admin.dao;

import app.xinqianmao.com.admin.common.entity.AdminLoginErrorLog;
import app.xinqianmao.com.common.dao.TenantBaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminLoginErrorLogMapper extends TenantBaseMapper<AdminLoginErrorLog> {
}
