/**
 * File: SkuSaveRequest.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * SKU save request.
 */
@Data
@Schema(description = "SKU保存请求")
public class SkuSaveRequest {

    private BigDecimal price;
    private BigDecimal oldPrice;
    private Integer inventory;
    private String picture;
    private List<SpecValueItem> specs;

    @Data
    @Schema(description = "规格值")
    public static class SpecValueItem {
        private String name;
        private String valueName;
    }
}
