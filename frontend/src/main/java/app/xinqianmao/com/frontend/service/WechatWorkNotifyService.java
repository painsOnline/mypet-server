/**
 * File: WechatWorkNotifyService.java
 * Author: system
 * Date: 2026-06-06
 */
package app.xinqianmao.com.frontend.service;

import app.xinqianmao.com.common.auth.TenantContext;
import app.xinqianmao.com.common.utils.DateTimeUtil;
import app.xinqianmao.com.common.utils.RegionUtil;
import app.xinqianmao.com.frontend.common.entity.*;
import app.xinqianmao.com.frontend.dao.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Send order notifications via WeChat Work webhook.
 * Runs asynchronously so notification failure never affects order flow.
 */
@Slf4j
@Service
public class WechatWorkNotifyService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private final OrderMapper orderMapper;
    private final OrderProductSkuMapper orderProductSkuMapper;
    private final OrderReceiverMapper orderReceiverMapper;
    private final ProductMapper productMapper;
    private final ShopMapper shopMapper;

    @Value("${wechat.work.webhook-key}")
    private String webhookKey;

    private static final String WEBHOOK_URL = "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=%s";

    public WechatWorkNotifyService(OrderMapper orderMapper,
                                   OrderProductSkuMapper orderProductSkuMapper,
                                   OrderReceiverMapper orderReceiverMapper,
                                   ProductMapper productMapper,
                                   ShopMapper shopMapper) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.orderMapper = orderMapper;
        this.orderProductSkuMapper = orderProductSkuMapper;
        this.orderReceiverMapper = orderReceiverMapper;
        this.productMapper = productMapper;
        this.shopMapper = shopMapper;
    }

    @Async
    public void sendNewOrderNotification(String orderNo, String tenantCode) {
        String prev = TenantContext.get();
        try {
            TenantContext.set(tenantCode);
            doSendNotification(orderNo, "<font color=\"info\">订单来袭</font>", "请及时处理订单 👨‍💻");
        } catch (Exception e) {
            log.error("Failed to send new order notification for {}: {}", orderNo, e.getMessage());
        } finally {
            if (prev != null) TenantContext.set(prev);
            else TenantContext.clear();
        }
    }

    @Async
    public void sendCancelOrderNotification(String orderNo, String tenantCode) {
        String prev = TenantContext.get();
        try {
            TenantContext.set(tenantCode);
            doSendNotification(orderNo, "<font color=\"warning\">订单取消</font>", "请及时联系客户获取原因 👨‍💻");
        } catch (Exception e) {
            log.error("Failed to send cancel order notification for {}: {}", orderNo, e.getMessage());
        } finally {
            if (prev != null) TenantContext.set(prev);
            else TenantContext.clear();
        }
    }

    private void doSendNotification(String orderNo, String action, String footer) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (order == null) {
            log.warn("Order not found for notification: {}", orderNo);
            return;
        }

        String shopName = getShopName();
        OrderReceiver receiver = orderReceiverMapper.selectById(orderNo);
        List<OrderProductSku> skus = orderProductSkuMapper.selectList(
                new LambdaQueryWrapper<OrderProductSku>().eq(OrderProductSku::getOrderNo, orderNo));

        String content = buildMarkdown(shopName, action, order, receiver, skus, footer);
        sendWebhook(content);
    }

    private String buildMarkdown(String shopName, String action, Order order,
                                  OrderReceiver receiver, List<OrderProductSku> skus,
                                  String footer) {
        StringBuilder sb = new StringBuilder();
        sb.append("#### 🐾 ").append(shopName).append(" - ").append(action).append(" @所有人\n\n");

        // Order info
        sb.append("> **订单号**：<font color=\"comment\">").append(order.getOrderNo()).append("</font>\n");
        sb.append("> **总金额**：<font color=\"warning\">¥").append(fmt(order.getActualPayMoney())).append("</font>\n");

        if (receiver != null) {
            sb.append("> **收货人**：").append(receiver.getReceiver()).append("\n");
            sb.append("> **联系电话**：").append(receiver.getContact()).append("\n");
            String addr = RegionUtil.getName(receiver.getProvinceCode())
                    + RegionUtil.getName(receiver.getCityCode())
                    + RegionUtil.getName(receiver.getCountyCode())
                    + " " + (receiver.getAddress() != null ? receiver.getAddress() : "");
            sb.append("> **收货地址**：").append(addr).append("\n");
        }

        sb.append("\n---\n\n#### 📦 商品明细\n\n");
        int index = 1;
        for (OrderProductSku sku : skus) {
            String productName = getProductName(sku.getProductId());
            sb.append("> **").append(index).append(". ").append(productName).append("**\n");
            BigDecimal price = sku.getPrice() != null ? sku.getPrice() : BigDecimal.ZERO;
            int count = sku.getCount() != null ? sku.getCount() : 0;
            sb.append("> - 单价：¥").append(fmt(price))
                    .append(" | 数量：").append(count)
                    .append(" | 小计：¥").append(fmt(price.multiply(BigDecimal.valueOf(count))))
                    .append("\n\n");
            index++;
        }

        sb.append("---\n\n> ").append(footer);
        return sb.toString();
    }

    private void sendWebhook(String content) {
        try {
            String url = String.format(WEBHOOK_URL, webhookKey);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("msgtype", "markdown");
            Map<String, String> markdown = new LinkedHashMap<>();
            markdown.put("content", content);
            body.put("markdown", markdown);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
            log.info("WechatWork webhook response: {}", resp.getBody());
        } catch (Exception e) {
            log.error("WechatWork webhook request failed: {}", e.getMessage());
        }
    }

    private String getShopName() {
        try {
            List<Shop> shops = shopMapper.selectList(new LambdaQueryWrapper<>());
            if (!shops.isEmpty() && shops.get(0).getName() != null && !shops.get(0).getName().isBlank()) {
                return shops.get(0).getName().trim();
            }
        } catch (Exception e) {
            log.warn("Failed to get shop name: {}", e.getMessage());
        }
        return "宠物用品店";
    }

    private String getProductName(String productId) {
        if (productId == null) return "";
        try {
            Product p = productMapper.selectById(productId);
            return p != null ? (p.getName() != null ? p.getName() : "") : "";
        } catch (Exception e) {
            return "";
        }
    }

    private String fmt(BigDecimal value) {
        if (value == null) return "0.00";
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
