/**
 * File: GoodsDetailResponse.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.frontend.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Goods/product detail response for mini-program frontend.
 */
@Data
@Schema(description = "商品详情响应")
public class GoodsDetailResponse {

    @Schema(description = "商品ID")
    private String id;

    @Schema(description = "商品名称")
    private String name;

    @Schema(description = "简介")
    private String desc;

    @Schema(description = "当前售价")
    private BigDecimal price;

    @Schema(description = "原价")
    private BigDecimal oldPrice;

    @Schema(description = "主图")
    private String picture;

    @Schema(description = "轮播图")
    private List<String> mainPictures;

    @Schema(description = "详情信息（属性+图片）")
    private DetailInfo details;

    @Schema(description = "SKU列表")
    private List<SkuItem> skus;

    @Schema(description = "规格组列表（用于SKU选择UI）")
    private List<SpecItem> specs;

    /**
     * Product detail information block.
     */
    @Data
    @Schema(description = "商品详情信息")
    public static class DetailInfo {

        @Schema(description = "属性列表")
        private List<PropertyItem> properties;

        @Schema(description = "详情图片列表")
        private List<String> pictures;

        @Schema(description = "商品详情HTML")
        private String detail;
    }

    /**
     * Product property item.
     */
    @Data
    @Schema(description = "商品属性")
    public static class PropertyItem {

        @Schema(description = "值ID")
        private String valueId;

        @Schema(description = "规格值显示名")
        private String valueName;

        @Schema(description = "规格ID")
        private String specId;

        @Schema(description = "规格名称")
        private String specName;
    }

    /**
     * SKU item.
     */
    @Data
    @Schema(description = "SKU")
    public static class SkuItem {

        @Schema(description = "SKU ID")
        private String id;

        @Schema(description = "虚拟库存")
        private Integer virtualInventory;

        @Schema(description = "原价")
        private BigDecimal oldPrice;

        @Schema(description = "图片")
        private String picture;

        @Schema(description = "售价")
        private BigDecimal price;

        @Schema(description = "规格值列表")
        private List<SpecValue> specs;
    }

    /**
     * Spec group item (e.g. "颜色", "规格") for SKU selection UI.
     */
    @Data
    @Schema(description = "规格组（SKU选择UI）")
    public static class SpecItem {

        @Schema(description = "规格组名称")
        private String specName;

        @Schema(description = "规格ID")
        private String specId;

        @Schema(description = "排序")
        private Integer sort;

        @Schema(description = "输入类型 (1=unique 2=single 3=multi)")
        private Integer inputType;

        @Schema(description = "规格值列表")
        private List<SpecValue> values;
    }

    /**
     * Spec value within a spec group, also used in SKU specs list and cart specs.
     */
    @Data
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    @Schema(description = "规格值")
    public static class SpecValue {
        @Schema(description = "规格组名称")
        private String specName;
        @Schema(description = "规格值显示名")
        private String valueName;
        @Schema(description = "规格ID")
        private String specId;
        @Schema(description = "值ID")
        private String valueId;
        @Schema(description = "是否可选")
        private Boolean available;
        @Schema(description = "图片")
        private String picture;
    }
}
