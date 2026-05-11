/**
 * File: ProductDetailResponse.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Product detail response.
 */
@Data
@Schema(description = "商品详情")
public class ProductDetailResponse {

    private String id;
    private String name;
    private String desc;
    private BigDecimal price;
    private BigDecimal oldPrice;
    private String picture;
    private List<String> mainPictures;
    private String detail;
    private String productType;
    private String productCategory;
    private String productBrand;
    private Integer sort;
    private Integer isEnable;
    private List<PropertyItem> properties;
    private List<SkuItem> skus;
    private List<SpecItem> specs;
    private String createTime;
    private String modifyTime;

    @Data
    @Schema(description = "商品属性")
    public static class PropertyItem {
        private String id;
        private String name;
        private String valueName;
    }

    @Data
    @Schema(description = "SKU")
    public static class SkuItem {
        private String id;
        private BigDecimal price;
        private BigDecimal oldPrice;
        private BigDecimal costPrice;
        private Integer inventory;
        private String barcode;
        private String picture;
        private List<SpecValue> specs;
    }

    @Data
    @Schema(description = "规格组")
    public static class SpecItem {
        private String name;
        private List<SpecValue> values;
    }

    @Data
    @Schema(description = "规格值")
    public static class SpecValue {
        private String name;
        private String valueName;
    }
}
