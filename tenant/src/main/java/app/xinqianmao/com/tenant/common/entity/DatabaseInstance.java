/**
 * File: DatabaseInstance.java
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
@TableName("c_database_instance")
public class DatabaseInstance {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String host;
    private Integer port;
    private String user;
    private String password;
    private LocalDateTime createTime;
    private LocalDateTime modifyTime;
}
