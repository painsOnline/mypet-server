/**
 * File: GoodsController.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.frontend.web.controller;

import app.xinqianmao.com.common.annotation.NoAuth;
import app.xinqianmao.com.common.exception.BizException;
import app.xinqianmao.com.common.result.Result;
import app.xinqianmao.com.frontend.common.entity.Product;
import app.xinqianmao.com.frontend.common.pojo.GoodsDetailResponse;
import app.xinqianmao.com.frontend.dao.ProductMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "商品", description = "商品详情")
@RestController
@RequestMapping("/frontend")
@RequiredArgsConstructor
public class GoodsController {

    private final ProductMapper productMapper;
    private final HomeController homeController;

    @NoAuth
    @Operation(summary = "获取商品详情")
    @GetMapping("/goods")
    public Result<GoodsDetailResponse> detail(@RequestParam String id) {
        Product product = productMapper.selectById(id);
        if (product == null) throw new BizException("404", "商品不存在");
        return Result.ok(homeController.buildGoodsDetail(product));
    }
}
