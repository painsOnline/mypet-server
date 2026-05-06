/**
 * File: SpecsSaveRequest.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * Spec create/update request.
 */
@Data
@Schema(description = "规格保存请求")
public class SpecsSaveRequest {

    @Schema(description = "商品类型ID")
    private String productType;

    @NotBlank(message = "规格名称不能为空")
    @Schema(description = "规格名称")
    private String name;

    @NotNull(message = "规格类型不能为空")
    @Schema(description = "1=SKU规格, 2=展示参数")
    private Integer type;

    @NotNull(message = "输入类型不能为空")
    @Schema(description = "1=唯一, 2=单选, 3=多选")
    private Integer inputType;

    @Schema(description = "可选值列表")
    private List<String> inputOptions;

    @Schema(description = "简介")
    private String desc;

    @Schema(description = "排序")
    private Integer sort;
}
