/**
 * File: HotProductResponse.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Hot product list response.
 */
@Data
@Schema(description = "热门商品项")
public class HotProductResponse {

    private String productId;
    private String productName;
    private String picture;
    private BigDecimal price;
    private BigDecimal oldPrice;
    private Integer sort;
}
