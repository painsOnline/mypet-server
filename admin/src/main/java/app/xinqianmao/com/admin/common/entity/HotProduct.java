/**
 * File: HotProduct.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * t_hot_products — hot/recommended products (no id column).
 */
@Data
@TableName("t_hot_products")
public class HotProduct {
    private String productId;
    private Integer sort;
    private LocalDateTime createTime;
    private LocalDateTime modifyTime;
}
