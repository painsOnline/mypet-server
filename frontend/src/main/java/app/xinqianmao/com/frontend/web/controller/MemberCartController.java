/**
 * File: MemberCartController.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.frontend.web.controller;

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

    @Operation(summary = "获取用户购物车")
    @GetMapping
    public Result<List<CartItemResponse>> getCart() {
        List<Cart> carts = cartMapper.selectList(new LambdaQueryWrapper<>());
        return Result.ok(carts.stream().map(this::toCartItemResponse).collect(Collectors.toList()));
    }

    @Operation(summary = "全量覆盖购物车")
    @PutMapping
    public Result<Boolean> syncCart(@RequestBody List<CartItemResponse> items) {
        cartMapper.delete(new LambdaQueryWrapper<>());
        if (items != null) {
            for (CartItemResponse item : items) {
                Cart cart = new Cart();
                cart.setSkuId(item.getSkuId());
                cart.setName(item.getName());
                cart.setPicture(item.getPicture() != null ? item.getPicture() : "");
                cart.setCount(item.getCount() != null ? item.getCount() : 1);
                cart.setPrice(item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO);
                cart.setNowPrice(item.getNowPrice() != null ? item.getNowPrice() : BigDecimal.ZERO);
                cart.setSelected(item.getSelected() != null && item.getSelected() ? 1 : 0);
                cart.setSpecs("[]");
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
        r.setPrice(cart.getPrice());
        r.setNowPrice(cart.getNowPrice());
        r.setSelected(cart.getSelected() != null && cart.getSelected() == 1);
        // Get current stock and check if product is still enabled
        ProductSku sku = skuMapper.selectById(cart.getSkuId());
        r.setStock(sku != null ? sku.getInventory() : 0);
        boolean effective = true;
        if (sku != null) {
            Product product = productMapper.selectById(sku.getProductId());
            effective = product != null && product.getIsEnable() != null && product.getIsEnable() == 1;
        }
        r.setIsEffective(effective);
        r.setAttrsText(extractAttrsText(cart.getSpecs()));
        return r;
    }

    @SuppressWarnings("unchecked")
    private String extractAttrsText(String specsJson) {
        if (specsJson == null || specsJson.isBlank()) return "";
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            List<java.util.Map<String, String>> list = mapper.readValue(specsJson, List.class);
            return list.stream()
                    .map(m -> m.get("name") + "：" + m.get("valueName"))
                    .collect(Collectors.joining("，"));
        } catch (Exception e) { return ""; }
    }
}
