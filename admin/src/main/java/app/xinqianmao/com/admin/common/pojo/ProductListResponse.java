/**
 * File: ProductListResponse.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Product list item response.
 */
@Data
@Schema(description = "商品列表项")
public class ProductListResponse {

    private String id;
    private String name;
    private String desc;
    private BigDecimal price;
    private BigDecimal oldPrice;
    private String picture;
    private String categoryName;
    private String typeName;
    private Integer sort;
    private Integer salesCount;
    private Integer isEnable;
    private Boolean isHot;
    private Boolean isActive;
    private String createTime;
}
