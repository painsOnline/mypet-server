/**
 * File: Order.java
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
@TableName("t_order")
public class Order extends BaseEntity {
    private Integer orderType;
    private Integer orderStatus;
    private String productType;
    private BigDecimal totalMoney;
    private BigDecimal actualPayMoney;
    private BigDecimal payMoney;
}
