/**
 * File: HotProduct.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * t_hot_products — hot/recommended products.
 */
@Data
@TableName("t_hot_products")
public class HotProduct {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String productId;
    private Integer sort;
    private LocalDateTime createTime;
    private LocalDateTime modifyTime;
}
