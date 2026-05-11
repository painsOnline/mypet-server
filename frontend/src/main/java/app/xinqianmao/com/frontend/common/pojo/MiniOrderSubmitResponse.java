/**
 * File: MiniOrderSubmitResponse.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.frontend.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Submit order response.
 */
@Data
@Schema(description = "提交订单响应")
public class MiniOrderSubmitResponse {

    @Schema(description = "新生成的订单ID")
    private String id;

    @Schema(description = "订单号")
    private String orderNo;
}
