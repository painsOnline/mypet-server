/**
 * File: HotSortRequest.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * Hot product sort request.
 */
@Data
@Schema(description = "热门商品排序请求")
public class HotSortRequest {

    @Schema(description = "排序列表")
    private List<SortItem> items;

    @Data
    @Schema(description = "排序项")
    public static class SortItem {
        @Schema(description = "商品ID")
        private String productId;
        @Schema(description = "排序值")
        private Integer sort;
    }
}
