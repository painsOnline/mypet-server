/**
 * File: SpecValuesUpdateRequest.java
 * Author: system
 * Date: 2026-05-04
 */
package app.xinqianmao.com.admin.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "更新规格值请求")
public class SpecValuesUpdateRequest {

    @Schema(description = "规格值列表")
    private List<String> inputOptions;
}
