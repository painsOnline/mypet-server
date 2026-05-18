/**
 * File: ProductSpecsValue.java
 * Author: system
 * Date: 2026-05-16
 */
package app.xinqianmao.com.frontend.common.entity;

import app.xinqianmao.com.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_product_specs_value")
public class ProductSpecsValue extends BaseEntity {
    private String specsId;
    private String valueName;
    private Integer sort;
}
