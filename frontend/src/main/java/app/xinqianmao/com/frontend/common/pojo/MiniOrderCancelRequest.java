/**
 * File: MiniOrderCancelRequest.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.frontend.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Cancel order request.
 */
@Data
@Schema(description = "取消订单请求")
public class MiniOrderCancelRequest {

    @NotBlank(message = "取消原因不能为空")
    @Schema(description = "取消原因", example = "不想要了")
    private String cancelReason;
}
