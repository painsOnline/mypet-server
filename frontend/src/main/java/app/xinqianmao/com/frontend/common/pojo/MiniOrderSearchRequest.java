/**
 * File: MiniOrderSearchRequest.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.frontend.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Mini-program order list search request.
 */
@Data
@Schema(description = "小程序订单列表搜索请求")
public class MiniOrderSearchRequest {

    @Schema(description = "页码", example = "1")
    private Integer page;

    @Schema(description = "每页条数", example = "5")
    private Integer pageSize;

    @Schema(description = "订单状态：0=全部，1=待配送，2=配送中，3=已收货，4=已完成，5=已取消")
    private Integer orderState;
}
