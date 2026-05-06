/**
 * File: TypeWithSpecsResponse.java
 * Author: system
 * Date: 2026-05-04
 */
package app.xinqianmao.com.admin.common.pojo;

import app.xinqianmao.com.admin.common.entity.ProductType;
import app.xinqianmao.com.admin.common.entity.ProductSpecs;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Product type with its nested specs, for the type management page expandable table.
 */
@Data
@Schema(description = "商品类型（含规格列表）")
public class TypeWithSpecsResponse {

    @Schema(description = "类型ID")
    private String id;

    @Schema(description = "类型名称")
    private String name;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "修改时间")
    private LocalDateTime modifyTime;

    @Schema(description = "规格列表")
    private List<ProductSpecs> specs;

    @Schema(description = "该类型下商品数量")
    private long productCount;

    public static TypeWithSpecsResponse from(ProductType type, List<ProductSpecs> specs, long productCount) {
        TypeWithSpecsResponse r = new TypeWithSpecsResponse();
        r.setId(type.getId());
        r.setName(type.getName());
        r.setSort(type.getSort());
        r.setCreateTime(type.getCreateTime());
        r.setModifyTime(type.getModifyTime());
        r.setSpecs(specs);
        r.setProductCount(productCount);
        return r;
    }
}
