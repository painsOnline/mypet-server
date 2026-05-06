/**
 * File: BannerResponse.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.frontend.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Banner / carousel item response.
 */
@Data
@Schema(description = "Banner轮播图项")
public class BannerResponse {

    @Schema(description = "Banner ID")
    private String id;

    @Schema(description = "图片URL")
    private String imgUrl;

    @Schema(description = "点击跳转链接")
    private String hrefUrl;

    @Schema(description = "展示位置类型：1=首页 2=分类页")
    private Integer type;
}
