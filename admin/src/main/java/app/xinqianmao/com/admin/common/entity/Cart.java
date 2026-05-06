/**
 * File: Cart.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.common.entity;

import app.xinqianmao.com.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * t_cart — shopping cart item.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_cart")
public class Cart extends BaseEntity {
    private String skuId;
    private String name;
    private String specs;
    private Integer count;
    private BigDecimal price;
    private BigDecimal nowPrice;
    private String picture;
    private Integer selected;
}
