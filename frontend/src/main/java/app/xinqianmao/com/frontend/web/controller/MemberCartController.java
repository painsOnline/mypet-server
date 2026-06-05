/**
 * File: MemberCartController.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.frontend.web.controller;

import app.xinqianmao.com.common.annotation.NoAuth;
import app.xinqianmao.com.common.auth.UserContext;
import app.xinqianmao.com.common.result.Result;
import app.xinqianmao.com.common.utils.DateTimeUtil;
import app.xinqianmao.com.common.utils.ImageUrlUtil;
import app.xinqianmao.com.frontend.common.entity.Cart;
import app.xinqianmao.com.frontend.common.entity.ProductSku;
import app.xinqianmao.com.frontend.common.pojo.CartItemResponse;
import app.xinqianmao.com.frontend.common.entity.Product;
import app.xinqianmao.com.frontend.dao.CartMapper;
import app.xinqianmao.com.frontend.dao.ProductMapper;
import app.xinqianmao.com.frontend.dao.ProductSkuMapper;
import app.xinqianmao.com.frontend.web.controller.HomeController;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "购物车", description = "用户购物车管理")
@RestController
@RequestMapping("/frontend/member/cart")
@RequiredArgsConstructor
public class MemberCartController {

    private final CartMapper cartMapper;
    private final ProductSkuMapper skuMapper;
    private final ProductMapper productMapper;
    private final ImageUrlUtil imageUrlUtil;

    private String currentMemberId() {
        return UserContext.getRequiredUserId();
    }

    @NoAuth
    @Operation(summary = "获取用户购物车（未登录返回空列表）")
    @GetMapping
    public Result<List<CartItemResponse>> getCart() {
        String memberId;
        try {
            memberId = UserContext.getRequiredUserId();
        } catch (Exception e) {
            return Result.ok(List.of());
        }
        List<Cart> carts = cartMapper.selectList(
                new LambdaQueryWrapper<Cart>().eq(Cart::getMemberId, memberId));
        return Result.ok(carts.stream().map(this::toCartItemResponse).collect(Collectors.toList()));
    }

    @Operation(summary = "全量覆盖购物车")
    @PutMapping
    public Result<Boolean> syncCart(@RequestBody List<CartItemResponse> items) {
        String memberId = currentMemberId();
        cartMapper.delete(new LambdaQueryWrapper<Cart>().eq(Cart::getMemberId, memberId));
        if (items != null) {
            for (CartItemResponse item : items) {
                Cart cart = new Cart();
                cart.setMemberId(memberId);
                cart.setSkuId(item.getSkuId());
                cart.setName(item.getName());
                cart.setPicture(item.getPicture() != null ? item.getPicture() : "");
                cart.setCount(item.getCount() != null ? item.getCount() : 1);
                cart.setSelected(item.getSelected() != null && item.getSelected() ? 1 : 0);
                // Serialize specs to snake_case JSON for DB storage
                if (item.getSpecs() != null && !item.getSpecs().isEmpty()) {
                    java.util.List<java.util.Map<String, String>> specsList = new java.util.ArrayList<>();
                    for (CartItemResponse.SpecItem sp : item.getSpecs()) {
                        java.util.Map<String, String> m = new java.util.LinkedHashMap<>();
                        if (sp.getSpecId() != null) m.put("spec_id", sp.getSpecId());
                        if (sp.getSpecName() != null) m.put("spec_name", sp.getSpecName());
                        if (sp.getValueId() != null) m.put("value_id", sp.getValueId());
                        if (sp.getValueName() != null) m.put("value_name", sp.getValueName());
                        specsList.add(m);
                    }
                    try {
                        cart.setSpecs(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(specsList));
                    } catch (Exception e) { cart.setSpecs("[]"); }
                } else {
                    cart.setSpecs("[]");
                }
                cart.setCreateTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
                cartMapper.insert(cart);
            }
        }
        return Result.ok(true);
    }

    private CartItemResponse toCartItemResponse(Cart cart) {
        CartItemResponse r = new CartItemResponse();
        r.setId(cart.getId());
        r.setSkuId(cart.getSkuId());
        r.setName(cart.getName());
        r.setPicture(imageUrlUtil.fullUrl(cart.getPicture()));
        r.setCount(cart.getCount());
        // Prices from SKU (dynamic)
        ProductSku sku = skuMapper.selectById(cart.getSkuId());
        r.setPrice(sku != null ? sku.getPrice() : BigDecimal.ZERO);
        r.setNowPrice(sku != null ? sku.getPrice() : BigDecimal.ZERO);
        r.setSelected(cart.getSelected() != null && cart.getSelected() == 1);
        r.setStock(sku != null ? sku.getVirtualInventory() : 0);
        boolean effective = true;
        if (sku != null) {
            Product product = productMapper.selectById(sku.getProductId());
            effective = product != null && product.getIsEnable() != null && product.getIsEnable() == 1;
        }
        r.setIsEffective(effective);
        r.setAttrsText(extractAttrsText(cart.getSpecs()));
        r.setSpecs(parseCartSpecs(cart.getSpecs()));
        return r;
    }

    private List<CartItemResponse.SpecItem> parseCartSpecs(String specsJson) {
        if (specsJson == null || specsJson.isBlank()) return List.of();
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            @SuppressWarnings("unchecked")
            List<java.util.Map<String, String>> list = mapper.readValue(specsJson, List.class);
            return list.stream().map(m -> {
                CartItemResponse.SpecItem si = new CartItemResponse.SpecItem();
                si.setSpecName(m.get("spec_name"));
                si.setValueName(m.get("value_name"));
                si.setSpecId(m.get("spec_id"));
                si.setValueId(m.get("value_id"));
                return si;
            }).collect(Collectors.toList());
        } catch (Exception e) { return List.of(); }
    }

    @SuppressWarnings("unchecked")
    private String extractAttrsText(String specsJson) {
        if (specsJson == null || specsJson.isBlank()) return "";
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            List<java.util.Map<String, String>> list = mapper.readValue(specsJson, List.class);
            return list.stream()
                    .map(m -> {
                        String n = m.get("spec_name");
                        String v = m.get("value_name");
                        if (n == null) n = ""; if (v == null) v = "";
                        return n + "：" + v;
                    })
                    .collect(Collectors.joining("，"));
        } catch (Exception e) { return ""; }
    }
}
