/**
 * File: MemberCartService.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.frontend.service;

import app.xinqianmao.com.common.utils.DateTimeUtil;
import app.xinqianmao.com.common.utils.UUIDUtil;
import app.xinqianmao.com.frontend.common.entity.Cart;
import app.xinqianmao.com.frontend.common.entity.ProductSku;
import app.xinqianmao.com.frontend.common.pojo.CartItemResponse;
import app.xinqianmao.com.frontend.dao.CartMapper;
import app.xinqianmao.com.frontend.dao.ProductSkuMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Shopping cart service for mini-program frontend.
 * Currently, t_cart has no user FK column; all cart items are treated as
 * belonging to the authenticated user of the current tenant.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberCartService {

    private final CartMapper cartMapper;
    private final ProductSkuMapper productSkuMapper;

    /**
     * Get all cart items for the current user.
     * Since t_cart has no user_id column, all rows in t_cart are returned.
     */
    public List<CartItemResponse> getCart(String userId) {
        List<Cart> cartItems = cartMapper.selectList(new LambdaQueryWrapper<>());
        if (cartItems.isEmpty()) {
            return List.of();
        }

        // Collect all SKU IDs to fetch current stock info
        Set<String> skuIds = cartItems.stream()
                .map(Cart::getSkuId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, ProductSku> skuMap;
        if (!skuIds.isEmpty()) {
            List<ProductSku> skus = productSkuMapper.selectBatchIds(skuIds);
            skuMap = skus.stream().collect(Collectors.toMap(ProductSku::getId, s -> s, (a, b) -> a));
        } else {
            skuMap = new HashMap<>();
        }

        return cartItems.stream().map(cart -> {
            CartItemResponse item = new CartItemResponse();
            item.setId(cart.getSkuId() != null ? getProductIdForSku(cart.getSkuId(), skuMap) : null);
            item.setSkuId(cart.getSkuId());
            item.setName(cart.getName());
            item.setPicture(cart.getPicture());
            item.setCount(cart.getCount());
            item.setPrice(cart.getPrice());
            item.setNowPrice(cart.getNowPrice());

            // Current stock from product_sku
            ProductSku sku = skuMap.get(cart.getSkuId());
            item.setStock(sku != null ? sku.getInventory() : 0);
            item.setIsEffective(sku != null);

            item.setSelected(cart.getSelected() != null && cart.getSelected() == 1);
            item.setAttrsText(extractAttrsText(cart.getSpecs()));
            return item;
        }).collect(Collectors.toList());
    }

    /**
     * Synchronize cart: delete all existing items and re-insert from the request list.
     * This is a full replacement strategy matching the "local-first + backend sync" approach
     * used by the mini-program frontend.
     */
    @Transactional
    public void syncCart(String userId, List<CartItemResponse> items) {
        // Delete all existing cart items
        cartMapper.delete(new LambdaQueryWrapper<>());

        // Re-insert
        if (items != null && !items.isEmpty()) {
            for (CartItemResponse item : items) {
                Cart cart = new Cart();
                cart.setId(UUIDUtil.uuid());
                cart.setSkuId(item.getSkuId());
                cart.setName(item.getName());
                cart.setSpecs(item.getAttrsText());
                cart.setCount(item.getCount());
                cart.setPrice(item.getPrice());
                cart.setNowPrice(item.getNowPrice());
                cart.setPicture(item.getPicture());
                cart.setSelected(Boolean.TRUE.equals(item.getSelected()) ? 1 : 0);
                cart.setCreateTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
                cartMapper.insert(cart);
            }
        }
    }

    // --- Helper methods ---

    /**
     * Get the product ID for a given SKU ID.
     */
    private String getProductIdForSku(String skuId, Map<String, ProductSku> skuMap) {
        if (skuId == null) return null;
        ProductSku sku = skuMap.get(skuId);
        return sku != null ? sku.getProductId() : null;
    }

    /**
     * Extract human-readable attrs text from specs JSON or plain text.
     */
    @SuppressWarnings("unchecked")
    private String extractAttrsText(String specs) {
        if (specs == null || specs.isBlank()) return "";
        // If it's JSON, parse it
        String trimmed = specs.strip();
        if (trimmed.startsWith("[")) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                List<Map<String, String>> list = mapper.readValue(trimmed, List.class);
                return list.stream()
                        .map(m -> m.get("name") + "：" + m.get("valueName"))
                        .collect(Collectors.joining("，"));
            } catch (Exception e) {
                // fall through to return as-is
            }
        }
        return specs;
    }
}
