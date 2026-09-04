/**
 * File: ShopLlmConfigService.java
 * Author: system
 * Date: 2026-06-14
 */
package app.xinqianmao.com.admin.service;

import app.xinqianmao.com.admin.common.entity.ShopLlmConfig;
import app.xinqianmao.com.admin.common.pojo.ShopLlmConfigSaveRequest;
import app.xinqianmao.com.admin.dao.ShopLlmConfigMapper;
import app.xinqianmao.com.common.exception.BizException;
import app.xinqianmao.com.common.utils.DateTimeUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShopLlmConfigService {

    private final ShopLlmConfigMapper mapper;

    public ShopLlmConfig getConfig() {
        List<ShopLlmConfig> list = mapper.selectList(new LambdaQueryWrapper<>());
        return list.isEmpty() ? null : list.get(0);
    }

    public void saveConfig(ShopLlmConfigSaveRequest req) {
        if (req.getProvider() == null || req.getProvider().isBlank()) {
            throw new BizException("400", "模型提供商不能为空");
        }
        if (req.getApiKey() == null || req.getApiKey().isBlank()) {
            throw new BizException("400", "API密钥不能为空");
        }
        if (req.getModelName() == null || req.getModelName().isBlank()) {
            throw new BizException("400", "模型名称不能为空");
        }
        if (req.getBaseUrl() == null || req.getBaseUrl().isBlank()) {
            throw new BizException("400", "API地址不能为空");
        }
        if (req.getTemperature() != null) {
            BigDecimal t = req.getTemperature();
            if (t.compareTo(BigDecimal.ZERO) < 0 || t.compareTo(new BigDecimal("2")) > 0) {
                throw new BizException("400", "温度取值范围为0-2");
            }
        }
        if (req.getMaxTokens() != null && req.getMaxTokens() < 1) {
            throw new BizException("400", "最大token数必须大于0");
        }
        if (req.getTimeoutSeconds() != null && req.getTimeoutSeconds() < 1) {
            throw new BizException("400", "超时时间必须大于0");
        }
        if (req.getMaxRetries() != null && req.getMaxRetries() < 0) {
            throw new BizException("400", "最大重试次数不能小于0");
        }

        List<ShopLlmConfig> list = mapper.selectList(new LambdaQueryWrapper<>());
        ShopLlmConfig config;
        if (list.isEmpty()) {
            config = new ShopLlmConfig();
            config.setId(UUID.randomUUID().toString());
            config.setCreateTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
        } else {
            config = list.get(0);
            config.setModifyTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
        }

        config.setProvider(req.getProvider());
        config.setApiKey(req.getApiKey());
        config.setModelName(req.getModelName());
        config.setBaseUrl(req.getBaseUrl());
        config.setTemperature(req.getTemperature() != null ? req.getTemperature() : new BigDecimal("0.3"));
        config.setMaxTokens(req.getMaxTokens() != null ? req.getMaxTokens() : 4096);
        config.setTimeoutSeconds(req.getTimeoutSeconds() != null ? req.getTimeoutSeconds() : 60);
        config.setMaxRetries(req.getMaxRetries() != null ? req.getMaxRetries() : 3);

        if (list.isEmpty()) {
            mapper.insert(config);
        } else {
            mapper.updateById(config);
        }
    }
}
