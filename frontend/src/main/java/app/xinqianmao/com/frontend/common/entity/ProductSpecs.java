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
    private String name;
    private Integer type;
    private Integer inputType;
    @TableField(exist = false)
    private List<String> inputOptions;
    @TableField(exist = false)
    private List<ProductSpecsValue> valuesList;
    @TableField("\"desc\"")
    private String desc;
    private Integer scope;
    private Integer sort;
}
