/**
 * File: ProductSpecs.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.frontend.common.entity;

import app.xinqianmao.com.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_product_specs")
public class ProductSpecs extends BaseEntity {
    private String productType;
    private String name;
    private Integer type;
    private Integer inputType;
    @TableField(typeHandler = app.xinqianmao.com.common.dao.ListStringTypeHandler.class)
    private List<String> inputOptions;
    @TableField("\"desc\"")
    private String desc;
    private Integer sort;
}
