/**
 * File: OrderProduct.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.frontend.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("t_order_products")
public class OrderProduct {
    private String orderId;
    private String productId;
    private String productType;
    private String productCategory;
    private String name;
    @TableField("\"desc\"")
    private String desc;
    private BigDecimal price;
    private BigDecimal oldPrice;
    @TableField(typeHandler = app.xinqianmao.com.common.dao.ListStringTypeHandler.class)
    private List<String> mainPictures;
    private String picture;
    private String detail;
    private Integer sort;
    private LocalDateTime createTime;
    private LocalDateTime modifyTime;
}
