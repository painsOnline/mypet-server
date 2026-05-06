/**
 * File: Order.java
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
 * t_order — order main table.
 * order_type: 0=pre, 1=confirmed. order_status: 1-5.
 */
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
