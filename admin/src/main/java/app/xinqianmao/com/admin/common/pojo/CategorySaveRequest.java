/**
 * File: CategorySaveRequest.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Category create/update request.
 */
@Data
@Schema(description = "分类保存请求")
public class CategorySaveRequest {

    @NotBlank(message = "分类名称不能为空")
    @Schema(description = "分类名称")
    private String name;

    @Schema(description = "分类图片URL")
    private String picture;

    @Schema(description = "排序", example = "0")
    private Integer sort;
}
