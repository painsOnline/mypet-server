/**
 * File: ProductSku.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.common.entity;

import app.xinqianmao.com.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * t_product_sku — product SKU with price, inventory, and spec combination.
 * specs is stored as JSON.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_product_sku")
public class ProductSku extends BaseEntity {
    private String productId;
    private String productType;
    private BigDecimal price;
    private BigDecimal oldPrice;
    private Integer inventory;
    private String picture;
    /** JSON string: [{name, valueName}] */
    @TableField(typeHandler = app.xinqianmao.com.common.dao.JsonTypeHandler.class)
    private String specs;
}
