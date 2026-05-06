/**
 * File: Cart.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.frontend.common.entity;

import app.xinqianmao.com.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_cart")
public class Cart extends BaseEntity {
    private String skuId;
    private String name;
    @TableField(typeHandler = app.xinqianmao.com.common.dao.JsonTypeHandler.class)
    private String specs;
    private Integer count;
    private BigDecimal price;
    private BigDecimal nowPrice;
    private String picture;
    private Integer selected;
}
