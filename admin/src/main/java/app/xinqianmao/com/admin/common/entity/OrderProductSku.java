/**
 * File: OrderProductSku.java
 * Author: system
 * Date: 2026-05-10
 */
package app.xinqianmao.com.admin.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_order_product_skus")
public class OrderProductSku {
    private String orderNo;
    private String skuId;
    private String productId;
    private String barcode;
    private BigDecimal price;
    private BigDecimal oldPrice;
    private BigDecimal costPrice;
    private BigDecimal profitMoney;
    private Integer count;
    private String picture;
    @TableField(typeHandler = app.xinqianmao.com.common.dao.JsonTypeHandler.class)
    private String specs;
    private LocalDateTime createTime;
    private LocalDateTime modifyTime;
}
