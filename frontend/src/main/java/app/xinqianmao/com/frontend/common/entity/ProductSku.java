/**
 * File: ProductSku.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.frontend.common.entity;

import app.xinqianmao.com.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

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
    private String specs;
}
