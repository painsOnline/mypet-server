package app.xinqianmao.com.admin.common.pojo;

import app.xinqianmao.com.admin.common.entity.ProductCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "分类列表项（含商品数量）")
public class CategoryListResponse {
    private String id;
    private String name;
    private String picture;
    private Integer sort;
    private String createTime;
    private String modifyTime;
    private long productCount;

    public static CategoryListResponse from(ProductCategory cat, long count) {
        CategoryListResponse r = new CategoryListResponse();
        r.setId(cat.getId());
        r.setName(cat.getName());
        r.setPicture(cat.getPicture());
        r.setSort(cat.getSort());
        r.setCreateTime(cat.getCreateTime() != null ? cat.getCreateTime().toString() : null);
        r.setModifyTime(cat.getModifyTime() != null ? cat.getModifyTime().toString() : null);
        r.setProductCount(count);
        return r;
    }
}
