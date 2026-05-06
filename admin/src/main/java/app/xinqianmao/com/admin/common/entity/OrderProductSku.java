/**
 * File: OrderProductSku.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * t_order_product_skus — SKU snapshot at order time.
 * Composite key (order_id, sku_id). inventory field stores purchase quantity.
 */
@Data
@TableName("t_order_product_skus")
public class OrderProductSku {
    private String orderId;
    private String skuId;
    private String productId;
    private String productType;
    private BigDecimal price;
    private BigDecimal oldPrice;
    /** Purchase quantity */
    private Integer inventory;
    private String picture;
    /** JSON string: [{name, valueName}] */
    @TableField(typeHandler = app.xinqianmao.com.common.dao.JsonTypeHandler.class)
    private String specs;
    private LocalDateTime createTime;
    private LocalDateTime modifyTime;
}
