/**
 * File: ProductCategory.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.common.entity;

import app.xinqianmao.com.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * t_product_category — product classification.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_product_category")
public class ProductCategory extends BaseEntity {
    private String name;
    private String picture;
    private Integer sort;
    private Integer isDelete;
}
