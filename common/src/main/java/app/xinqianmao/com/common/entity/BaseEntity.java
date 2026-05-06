/**
 * File: BaseEntity.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Base entity with UUID primary key and timestamp fields.
 * All business table entities should extend this.
 */
@Data
public abstract class BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private LocalDateTime createTime;

    private LocalDateTime modifyTime;
}
