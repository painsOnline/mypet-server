/**
 * File: ProductSpecs.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.common.entity;

import app.xinqianmao.com.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * t_product_specs — spec definition per product type.
 * type: 1=SKU, 2=display. input_type: 1=unique, 2=single, 3=multi.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_product_specs")
public class ProductSpecs extends BaseEntity {
    private String productType;
    private String name;
    private Integer type;
    private Integer inputType;
    /** PostgreSQL array mapped via MyBatis-Plus type handler */
    @TableField(typeHandler = app.xinqianmao.com.common.dao.ListStringTypeHandler.class)
    private List<String> inputOptions;
    @TableField("\"desc\"")
    private String desc;
    private Integer sort;

    /** Transient: option values already referenced by existing SKUs (non-persistent) */
    @TableField(exist = false)
    private List<String> usedOptions;
}
