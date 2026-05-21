/**
 * File: TenantAdmin.java
 * Author: system
 * Date: 2026-05-21
 */
package app.xinqianmao.com.tenant.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_admin")
public class TenantAdmin {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String account;
    private String password;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;
    private LocalDateTime modifyTime;
}
