/**
 * File: ProductSaveRequest.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Product create/update request.
 */
@Data
@Schema(description = "商品保存请求")
public class ProductSaveRequest {

    @NotBlank(message = "商品名称不能为空")
    @Schema(description = "商品名称")
    private String name;

    @NotBlank(message = "商品类型不能为空")
    @Schema(description = "商品类型ID")
    private String productType;

    @NotBlank(message = "商品分类不能为空")
    @Schema(description = "商品分类ID")
    private String productCategory;

    @NotBlank(message = "商品品牌不能为空")
    @Schema(description = "商品品牌ID")
    private String productBrand;

    @Schema(description = "商品简介")
    private String desc;

    @NotNull(message = "售价不能为空")
    @Schema(description = "售价")
    private BigDecimal price;

    @NotNull(message = "原价不能为空")
    @Schema(description = "原价")
    private BigDecimal oldPrice;

    @Schema(description = "轮播图URL列表")
    private List<String> mainPictures;

    @NotBlank(message = "商品主图不能为空")
    @Schema(description = "商品主图")
    private String picture;

    @Schema(description = "商品详情HTML")
    private String detail;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "商品属性列表")
    private List<PropertyItem> properties;

    @Schema(description = "商品SKU列表")
    private List<SkuItem> skus;

    @Data
    @Schema(description = "商品属性")
    public static class PropertyItem {
        @Schema(description = "属性名")
        private String name;
        @Schema(description = "属性值")
        private String valueName;
    }

    @Data
    @Schema(description = "商品SKU")
    public static class SkuItem {
        @Schema(description = "SKU ID（编辑时传入，新增时为空）")
        private String id;
        @Schema(description = "价格")
        private BigDecimal price;
        @Schema(description = "原价")
        private BigDecimal oldPrice;
        @Schema(description = "库存")
        private Integer inventory;
        @Schema(description = "成本价")
        private BigDecimal costPrice;
        @Schema(description = "条形码")
        private String barcode;
        @Schema(description = "图片")
        private String picture;
        @Schema(description = "规格组合 JSON")
        private List<SpecValue> specs;

        @Data
        @Schema(description = "规格值")
        public static class SpecValue {
            @Schema(description = "规格名")
            private String name;
            @Schema(description = "规格值")
            private String valueName;
        }
    }
}
