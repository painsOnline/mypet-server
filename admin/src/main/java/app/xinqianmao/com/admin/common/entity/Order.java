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
import java.time.LocalDateTime;

/**
 * t_order — order main table.
 * order_type: 0=pre, 1=confirmed. order_status: 1-5.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_order")
public class Order extends BaseEntity {
    private String memberId;
    private String orderNo;
    private Integer orderType;
    private Integer orderStatus;
    private String productType;
    private BigDecimal totalMoney;
    private BigDecimal actualPayMoney;
    private BigDecimal payMoney;
    private BigDecimal profitMoney;
    private String buyerMessage;
    private Integer payChannel;
    private Integer payType;
    private Integer isDelete;
    private LocalDateTime payTime;
    private LocalDateTime deliveryTime;
    private LocalDateTime receiveTime;
    private LocalDateTime finishTime;
    private LocalDateTime cancelTime;
    private String sellerMessage;
}
