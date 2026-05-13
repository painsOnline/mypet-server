/**
 * File: ProductProperty.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.common.entity;

import app.xinqianmao.com.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * t_product_properties — display properties for a product.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_product_properties")
public class ProductProperty extends BaseEntity {
    private String productId;
    private String specsId;
    private String valueName;
    private Integer sort;
    private Integer isDelete;
}
