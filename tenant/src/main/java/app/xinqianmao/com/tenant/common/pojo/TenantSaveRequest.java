/**
 * File: TenantSaveRequest.java
 * Author: system
 * Date: 2026-05-21
 */
package app.xinqianmao.com.tenant.common.pojo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TenantSaveRequest {

    /** Tenant code (unique, required on create, not changeable on update) */
    @NotBlank(message = "租户code不能为空")
    private String code;

    @NotBlank(message = "租户名称不能为空")
    private String name;

    /** Database instance ID (required on create, not changeable on update) */
    @NotBlank(message = "数据库实例不能为空")
    private String databaseInstanceId;

    private Integer isDisable;
    private Integer isBussinessOpen;
    private BigDecimal freeShippingAmount;
}
