/**
 * File: UserSearchRequest.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * User search request.
 */
@Data
@Schema(description = "用户搜索请求")
public class UserSearchRequest {

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "加入开始时间")
    private String createTimeStart;

    @Schema(description = "加入结束时间")
    private String createTimeEnd;

    @Schema(description = "排序字段")
    private String sortBy;

    @Schema(description = "排序方向")
    private String sortOrder;

    @Schema(description = "页码", example = "1")
    private Long page = 1L;

    @Schema(description = "每页条数", example = "10")
    private Long pageSize = 10L;
}
