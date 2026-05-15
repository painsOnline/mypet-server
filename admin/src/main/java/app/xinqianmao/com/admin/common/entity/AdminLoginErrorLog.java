package app.xinqianmao.com.admin.common.entity;

import app.xinqianmao.com.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("c_admin_login_error_log")
public class AdminLoginErrorLog extends BaseEntity {
    private String tenantCode;
    private String account;
    private String errorType;
    private String loginIp;
}
