/**
 * File: InventoryLog.java
 * Author: system
 * Date: 2026-05-10
 */
package app.xinqianmao.com.frontend.common.entity;

import app.xinqianmao.com.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * t_inventory_log — inventory change log.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_inventory_log")
public class InventoryLog extends BaseEntity {
    private String skuId;
    private String barcode;
    private String orderNo;
    private String changeType;
    private Integer changeNum;
    private Integer beforeInventory;
    private Integer afterInventory;
    private String operator;
}
