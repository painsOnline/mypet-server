/**
 * File: TenantListResponse.java
 * Author: system
 * Date: 2026-05-21
 */
package app.xinqianmao.com.tenant.common.pojo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TenantListResponse {

    private String id;
    private String code;
    private String name;
    private String databaseInstanceId;
    private Integer isDisable;
    private Integer isBussinessOpen;
    private BigDecimal freeShippingAmount;
    private LocalDateTime createTime;
    private LocalDateTime modifyTime;

    /** Display field: name of the associated database instance */
    private String databaseInstanceName;
}
