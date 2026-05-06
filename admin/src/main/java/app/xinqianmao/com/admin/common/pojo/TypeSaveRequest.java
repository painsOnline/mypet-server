/**
 * File: TypeSaveRequest.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Product type create/update request.
 */
@Data
@Schema(description = "商品类型保存请求")
public class TypeSaveRequest {

    @NotBlank(message = "类型名称不能为空")
    @Schema(description = "类型名称")
    private String name;

    @Schema(description = "排序", example = "0")
    private Integer sort;
}
