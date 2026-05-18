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
 * t_product_specs — spec definition.
 * type: 1=SKU, 2=display. input_type: 1=unique, 2=single, 3=multi.
 * scope: 0=global, 1=shared, 2=private.
 * Values are stored in t_product_specs_value table.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_product_specs")
public class ProductSpecs extends BaseEntity {
    private String name;
    private Integer type;
    private Integer inputType;
    @TableField("\"desc\"")
    private String desc;
    private Integer scope;
    private Integer sort;

    /** Transient: option values loaded from t_product_specs_value */
    @TableField(exist = false)
    private List<String> inputOptions;

    /** Transient: option values with IDs loaded from t_product_specs_value */
    @TableField(exist = false)
    private List<ProductSpecsValue> valuesList;

    /** Transient: option values already referenced by existing SKUs (non-persistent) */
    @TableField(exist = false)
    private List<String> usedOptions;

    /** Transient: linked type names for global/shared specs (non-persistent) */
    @TableField(exist = false)
    private List<String> linkedTypeNames;
}
