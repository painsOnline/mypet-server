package app.xinqianmao.com.frontend.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "店铺详情")
public class ShopDetailResponse {
    private String id;
    private String name;
    private String logo;
    private BigDecimal freeShippingAmount;
    private String detail;
    private String contact;
    private List<BannerItem> banners;

    @Data
    @Schema(description = "Banner项")
    public static class BannerItem {
        private String imgUrl;
        private String hrefUrl;
        private Integer type;
        private Integer sort;
    }
}
