/**
 * File: ProductBrand.java
 * Author: system
 * Date: 2026-05-11
 */
package app.xinqianmao.com.frontend.common.entity;

import app.xinqianmao.com.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * t_product_brand — product brand.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_product_brand")
public class ProductBrand extends BaseEntity {
    private String brandName;
    private String brandEn;
    private String brandLogo;
    private String brandDesc;
    private Integer sort;
    private Integer isDelete;
}
