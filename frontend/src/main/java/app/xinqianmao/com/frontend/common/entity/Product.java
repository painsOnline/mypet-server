/**
 * File: Product.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.frontend.common.entity;

import app.xinqianmao.com.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_product")
public class Product extends BaseEntity {
    private String productType;
    private String productCategory;
    private String productBrand;
    private String name;
    @TableField("\"desc\"")
    private String desc;
    private BigDecimal price;
    private BigDecimal oldPrice;
    @TableField(typeHandler = app.xinqianmao.com.common.dao.ListStringTypeHandler.class)
    private List<String> mainPictures;
    private String picture;
    private String detail;
    private Integer sort;
    private String searchText;
    private Integer isEnable;
}
