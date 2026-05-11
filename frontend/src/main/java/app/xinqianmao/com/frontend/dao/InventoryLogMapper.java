package app.xinqianmao.com.frontend.dao;

import app.xinqianmao.com.common.dao.TenantBaseMapper;
import app.xinqianmao.com.frontend.common.entity.InventoryLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InventoryLogMapper extends TenantBaseMapper<InventoryLog> {
}
