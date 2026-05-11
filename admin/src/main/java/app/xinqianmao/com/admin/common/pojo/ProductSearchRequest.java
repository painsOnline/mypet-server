/**
 * File: ProductSearchRequest.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * Product search/filter request.
 */
@Data
@Schema(description = "商品搜索请求")
public class ProductSearchRequest {

    @Schema(description = "商品名称", example = "猫砂")
    private String name;

    @Schema(description = "分类ID（单选，兼容旧版）")
    private String categoryId;

    @Schema(description = "分类ID列表（多选）")
    private List<String> categoryIds;

    @Schema(description = "商品类型ID（单选，兼容旧版）")
    private String typeId;

    @Schema(description = "商品类型ID列表（多选）")
    private List<String> typeIds;

    @Schema(description = "商品品牌ID")
    private String brandId;

    @Schema(description = "条形码（支持后N位模糊匹配）")
    private String barcode;

    @Schema(description = "最低价格")
    private java.math.BigDecimal priceMin;

    @Schema(description = "最高价格")
    private java.math.BigDecimal priceMax;

    @Schema(description = "上架开始时间", example = "2026-01-01 00:00:00")
    private String createTimeStart;

    @Schema(description = "上架结束时间", example = "2026-12-31 23:59:59")
    private String createTimeEnd;

    @Schema(description = "是否热门推荐")
    private Boolean isHot;

    @Schema(description = "排序字段: price, createTime, salesCount")
    private String sortBy;

    @Schema(description = "是否上架: 1=上架 2=下架")
    private Integer isEnable;

    @Schema(description = "排序方向: asc, desc")
    private String sortOrder;

    @Schema(description = "页码", example = "1")
    private Long page = 1L;

    @Schema(description = "每页条数", example = "10")
    private Long pageSize = 10L;
}
