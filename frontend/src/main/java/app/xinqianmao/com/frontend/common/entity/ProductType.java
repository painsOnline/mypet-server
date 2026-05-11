/**
 * File: ProductType.java
 * Author: system
 * Date: 2026-05-11
 */
package app.xinqianmao.com.frontend.common.entity;

import app.xinqianmao.com.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * t_product_type — product type (defines specs template).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_product_type")
public class ProductType extends BaseEntity {
    private String name;
    private Integer sort;
    private Integer isDelete;
}
