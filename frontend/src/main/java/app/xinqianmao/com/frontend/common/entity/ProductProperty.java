/**
 * File: ProductProperty.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.frontend.common.entity;

import app.xinqianmao.com.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_product_properties")
public class ProductProperty extends BaseEntity {
    private String productId;
    private String productType;
    private String name;
    private String valueName;
    private Integer sort;
}
