/**
 * File: ShopLlmConfigSaveRequest.java
 * Author: system
 * Date: 2026-06-14
 */
package app.xinqianmao.com.admin.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "大模型配置请求")
public class ShopLlmConfigSaveRequest {

    @Schema(description = "模型提供商")
    private String provider;

    @Schema(description = "API密钥")
    private String apiKey;

    @Schema(description = "模型名称")
    private String modelName;

    @Schema(description = "API地址")
    private String baseUrl;

    @Schema(description = "温度(0-2)", allowableValues = {"range=[0.0, 2.0]"})
    private BigDecimal temperature;

    @Schema(description = "单次最大输出token")
    private Integer maxTokens;

    @Schema(description = "超时时间(秒)")
    private Integer timeoutSeconds;

    @Schema(description = "最大重试次数")
    private Integer maxRetries;
}
