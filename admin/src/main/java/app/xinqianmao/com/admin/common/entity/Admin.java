/**
 * File: Admin.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * t_admin — admin user (no id column, identified by account).
 */
@Data
@TableName("t_admin")
public class Admin {
    private String account;
    private String password;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;
    private LocalDateTime modifyTime;
}
