/**
 * File: OrderProductSku.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.frontend.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_order_product_skus")
public class OrderProductSku {
    private String orderId;
    private String skuId;
    private String productId;
    private String productType;
    private BigDecimal price;
    private BigDecimal oldPrice;
    private Integer inventory;
    private String picture;
    private String specs;
    private LocalDateTime createTime;
    private LocalDateTime modifyTime;
}
