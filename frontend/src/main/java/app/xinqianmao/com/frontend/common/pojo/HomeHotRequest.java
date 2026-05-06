/**
 * File: HomeHotRequest.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.frontend.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Hot products paging request.
 */
@Data
@Schema(description = "首页热门推荐分页请求")
public class HomeHotRequest {

    @Schema(description = "页码", example = "1")
    private Integer page;

    @Schema(description = "每页条数", example = "6")
    private Integer pageSize;
}
