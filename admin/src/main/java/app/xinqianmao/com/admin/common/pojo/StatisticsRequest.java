/**
 * File: StatisticsRequest.java
 * Author: system
 * Date: 2026-05-05
 */
package app.xinqianmao.com.admin.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Statistics query request with date range.
 */
@Data
@Schema(description = "统计查询请求")
public class StatisticsRequest {

    @Schema(description = "开始日期", example = "2026-04-28")
    private String startDate;

    @Schema(description = "结束日期", example = "2026-05-05")
    private String endDate;
}
