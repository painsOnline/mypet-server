/**
 * File: Shop.java
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
@TableName("t_shop")
public class Shop {
    private String id;
    private String name;
    private String logo;
    private BigDecimal freeShippingAmount;
    @TableField(typeHandler = app.xinqianmao.com.common.dao.JsonTypeHandler.class)
    private String banners;
    private String detail;
    private LocalDateTime createTime;
    private LocalDateTime modifyTime;
}
