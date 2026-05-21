/**
 * File: Tenant.java
 * Author: system
 * Date: 2026-05-08
 */
package app.xinqianmao.com.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Tenant entity — maps to c_tenant in mypet_config.
 * Note: c_tenant lives in the config DB, not tenant DBs.
 * Mapper queries must ensure the DataSource is routed to config.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("c_tenant")
public class Tenant extends BaseEntity {

    /** Tenant code, used as unique identifier in Tenant header */
    private String code;

    /** Tenant display name */
    private String name;

    /** FK to c_database_instance.id */
    private String databaseInstanceId;

    /** Free shipping threshold */
    private java.math.BigDecimal freeShippingAmount;

    /** 0=normal, 1=disabled */
    private Integer isDisable;

    /** 0=暂停营业, 1=正常营业 */
    private Integer isBussinessOpen;
}
