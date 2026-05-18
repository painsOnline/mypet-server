/**
 * File: Cart.java
 * Author: system
 * Date: 2026-05-10
 */
package app.xinqianmao.com.frontend.common.entity;

import app.xinqianmao.com.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * t_cart — shopping cart item.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_cart")
public class Cart extends BaseEntity {
    private String memberId;
    private String skuId;
    private String name;
    @TableField(typeHandler = app.xinqianmao.com.common.dao.JsonTypeHandler.class)
    private String specs;
    private Integer count;
    private String productId;
    private String picture;
    private Integer selected;
}
