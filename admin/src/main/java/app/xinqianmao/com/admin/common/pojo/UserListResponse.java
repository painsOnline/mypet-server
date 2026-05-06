/**
 * File: UserListResponse.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * User list item response with order statistics.
 */
@Data
@Schema(description = "用户列表项")
public class UserListResponse {

    private String id;
    private String mobile;
    private String nickname;
    private String avatar;
    private String createTime;
    private Long orderCount;
    private BigDecimal totalOrderAmount;
    private BigDecimal avgOrderAmount;
}
