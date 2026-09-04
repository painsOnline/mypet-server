/**
 * File: ShopLlmConfig.java
 * Author: system
 * Date: 2026-06-14
 */
package app.xinqianmao.com.admin.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_shop_llm_config")
public class ShopLlmConfig {

    private String id;
    private String provider;
    private String apiKey;
    private String modelName;
    private String baseUrl;
    private BigDecimal temperature;
    private Integer maxTokens;
    private Integer timeoutSeconds;
    private Integer maxRetries;
    private LocalDateTime createTime;
    private LocalDateTime modifyTime;
}
