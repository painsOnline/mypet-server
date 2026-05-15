package app.xinqianmao.com.admin.common.entity;

import app.xinqianmao.com.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("c_admin_login_lock")
public class AdminLoginLock extends BaseEntity {
    private String tenantCode;
    private String account;
    private LocalDateTime lockEndTime;
}
