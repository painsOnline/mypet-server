/**
 * File: ProductTypeSpecRel.java
 * Author: system
 * Date: 2026-05-13
 */
package app.xinqianmao.com.admin.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * t_product_type_spec_rel — product type ↔ spec relation.
 */
@Data
@TableName("t_product_type_spec_rel")
public class ProductTypeSpecRel {
    private String id;
    private String productType;
    private String specsId;
    private Integer sort;
    private LocalDateTime createTime;
    private LocalDateTime modifyTime;
}
