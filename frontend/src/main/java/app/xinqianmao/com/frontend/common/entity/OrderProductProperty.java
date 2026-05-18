/**
 * File: OrderProductProperty.java
 * Author: system
 * Date: 2026-05-11
 */
package app.xinqianmao.com.frontend.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * t_order_product_properties — order product property snapshot.
 */
@Data
@TableName("t_order_product_properties")
public class OrderProductProperty {
    private String orderNo;
    private String propertyId;
    private String productId;
    private String name;
    private String valueName;
    private String valueId;
    private Integer sort;
    private LocalDateTime createTime;
    private LocalDateTime modifyTime;
}
