/**
 * File: MemberOrderController.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.frontend.web.controller;

import app.xinqianmao.com.common.auth.UserContext;
import app.xinqianmao.com.common.enums.OrderStatusEnum;
import app.xinqianmao.com.common.exception.BizException;
import app.xinqianmao.com.common.result.PageResult;
import app.xinqianmao.com.common.result.Result;
import app.xinqianmao.com.common.utils.DateTimeUtil;
import app.xinqianmao.com.common.utils.ImageUrlUtil;
import app.xinqianmao.com.frontend.common.entity.*;
import app.xinqianmao.com.frontend.common.pojo.*;
import app.xinqianmao.com.frontend.dao.*;
import app.xinqianmao.com.frontend.web.controller.HomeController;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "订单", description = "用户订单管理")
@RestController
@RequestMapping("/frontend/member/order")
@RequiredArgsConstructor
public class MemberOrderController {

    private final OrderMapper orderMapper;
    private final OrderProductMapper orderProductMapper;
    private final OrderProductSkuMapper orderProductSkuMapper;
    private final OrderReceiverMapper orderReceiverMapper;
    private final ReceiverMapper receiverMapper;
    private final CartMapper cartMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper skuMapper;
    private final HomeController homeController;
    private final ImageUrlUtil imageUrlUtil;

    @Operation(summary = "获取订单列表")
    @GetMapping
    public Result<PageResult<MiniOrderListResponse>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "5") Integer pageSize,
            @RequestParam(required = false) Integer orderState) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (orderState != null && orderState != 0) {
            wrapper.eq(Order::getOrderStatus, orderState);
        }
        wrapper.orderByDesc(Order::getId);
        Page<Order> p = Page.of(page, pageSize);
        IPage<Order> orderPage = orderMapper.selectPage(p, wrapper);
        return Result.ok(PageResult.of(orderPage.convert(this::toListResponse)));
    }

    @Operation(summary = "获取订单详情")
    @GetMapping("/{id}")
    public Result<MiniOrderDetailResponse> detail(@PathVariable String id) {
        Order order = orderMapper.selectById(id);
        if (order == null) throw new BizException("404", "订单不存在");
        MiniOrderDetailResponse resp = new MiniOrderDetailResponse();
        resp.setId(order.getId());
        resp.setOrderState(order.getOrderStatus());
        OrderStatusEnum statusEnum = OrderStatusEnum.fromCode(order.getOrderStatus());
        resp.setOrderStateDesc(statusEnum != null ? statusEnum.getDesc() : "");
        resp.setCreateTime(DateTimeUtil.format(order.getCreateTime()));
        resp.setTotalMoney(order.getTotalMoney());
        resp.setPayMoney(order.getPayMoney());
        resp.setActualPayMoney(order.getActualPayMoney());

        OrderReceiver receiver = orderReceiverMapper.selectById(id);
        if (receiver != null) {
            resp.setReceiverContact(receiver.getReceiver());
            resp.setReceiverMobile(receiver.getContact());
            resp.setReceiverAddress(receiver.getProvinceCode() + receiver.getCityCode()
                    + receiver.getCountyCode() + receiver.getAddress());
        }

        List<OrderProductSku> skus = orderProductSkuMapper.selectList(
                new LambdaQueryWrapper<OrderProductSku>().eq(OrderProductSku::getOrderId, id));
        resp.setSkus(skus.stream().map(os -> {
            MiniOrderDetailResponse.SkuItem si = new MiniOrderDetailResponse.SkuItem();
            si.setId(os.getSkuId());
            si.setProductId(os.getProductId());
            si.setName(getProductName(os.getProductId()));
            si.setAttrsText(extractAttrsText(os.getSpecs()));
            si.setQuantity(os.getInventory());
            si.setPrice(os.getPrice());
            si.setOldPrice(os.getOldPrice());
            si.setPicture(imageUrlUtil.fullUrl(os.getPicture()));
            return si;
        }).collect(Collectors.toList()));
        resp.setTotalNum(skus.stream().mapToInt(os -> os.getInventory() != null ? os.getInventory() : 0).sum());

        return Result.ok(resp);
    }

    @Operation(summary = "确认收货")
    @PutMapping("/{id}/receipt")
    @Transactional
    public Result<MiniOrderListResponse> confirmReceipt(@PathVariable String id) {
        Order order = orderMapper.selectById(id);
        if (order == null) throw new BizException("404", "订单不存在");
        if (order.getOrderStatus() != 2) throw new BizException("400", "只有配送中的订单才能确认收货");
        order.setOrderStatus(3);
        order.setModifyTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
        orderMapper.updateById(order);
        return Result.ok(toListResponse(order));
    }

    @Operation(summary = "取消订单")
    @PutMapping("/{id}/cancel")
    @Transactional
    public Result<MiniOrderListResponse> cancel(@PathVariable String id,
                                                 @Valid @RequestBody MiniOrderCancelRequest request) {
        Order order = orderMapper.selectById(id);
        if (order == null) throw new BizException("404", "订单不存在");
        if (order.getOrderStatus() != 1) throw new BizException("400", "只有待配送的订单才能取消");
        order.setOrderStatus(5);
        order.setModifyTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
        orderMapper.updateById(order);
        return Result.ok(toListResponse(order));
    }

    @Operation(summary = "删除订单")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable String id) {
        Order order = orderMapper.selectById(id);
        if (order == null) throw new BizException("404", "订单不存在");
        if (order.getOrderStatus() != 4 && order.getOrderStatus() != 5) {
            throw new BizException("400", "只有已完成或已取消的订单才能删除");
        }
        orderMapper.deleteById(id);
        orderProductMapper.delete(new LambdaQueryWrapper<OrderProduct>().eq(OrderProduct::getOrderId, id));
        orderProductSkuMapper.delete(new LambdaQueryWrapper<OrderProductSku>().eq(OrderProductSku::getOrderId, id));
        return Result.ok(true);
    }

    @Operation(summary = "购物车结算预付单")
    @GetMapping("/pre")
    public Result<PreOrderResponse> preOrder() {
        List<Cart> selectedCarts = cartMapper.selectList(
                new LambdaQueryWrapper<Cart>().eq(Cart::getSelected, 1));
        if (selectedCarts.isEmpty()) throw new BizException("400", "请选择要购买的商品");

        PreOrderResponse resp = new PreOrderResponse();
        List<PreOrderResponse.ProductItem> products = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;
        BigDecimal totalPayPrice = BigDecimal.ZERO;

        for (Cart cart : selectedCarts) {
            ProductSku sku = skuMapper.selectById(cart.getSkuId());
            Product product = productMapper.selectById(sku != null ? sku.getProductId() : null);
            PreOrderResponse.ProductItem pp = new PreOrderResponse.ProductItem();
            pp.setId(product != null ? product.getId() : "");
            pp.setSkuId(cart.getSkuId());
            pp.setName(cart.getName());
            pp.setAttrsText(extractAttrsText(cart.getSpecs()));
            pp.setCount(cart.getCount());
            pp.setPrice(cart.getPrice());
            pp.setPayPrice(cart.getNowPrice());
            pp.setPicture(imageUrlUtil.fullUrl(cart.getPicture()));
            BigDecimal itemTotal = cart.getPrice().multiply(BigDecimal.valueOf(cart.getCount()));
            BigDecimal itemPayTotal = cart.getNowPrice().multiply(BigDecimal.valueOf(cart.getCount()));
            pp.setTotalPrice(itemTotal);
            pp.setTotalPayPrice(itemPayTotal);
            products.add(pp);
            totalPrice = totalPrice.add(itemTotal);
            totalPayPrice = totalPayPrice.add(itemPayTotal);
        }

        PreOrderResponse.Summary summary = new PreOrderResponse.Summary();
        summary.setTotalPrice(totalPrice);
        summary.setPostFee(BigDecimal.ZERO);
        summary.setTotalPayPrice(totalPayPrice);
        resp.setSummary(summary);

        // Addresses
        List<Receiver> receivers = receiverMapper.selectList(new LambdaQueryWrapper<>());
        resp.setUserAddresses(receivers.stream().map(r -> {
            AddressResponse ar = new AddressResponse();
            ar.setId(r.getId());
            ar.setReceiver(r.getReceiver());
            ar.setContact(r.getContact());
            ar.setProvinceCode(r.getProvinceCode());
            ar.setCityCode(r.getCityCode());
            ar.setCountyCode(r.getCountyCode());
            ar.setAddress(r.getAddress());
            ar.setIsDefault(r.getIsDefault());
            ar.setFullLocation(r.getProvinceCode() + " " + r.getCityCode() + " " + r.getCountyCode());
            return ar;
        }).collect(Collectors.toList()));
        resp.setProducts(products);

        return Result.ok(resp);
    }

    @Operation(summary = "立即购买预付单")
    @GetMapping("/pre/now")
    public Result<PreOrderResponse> preOrderNow(@RequestParam String skuId,
                                                 @RequestParam(defaultValue = "1") Integer count,
                                                 @RequestParam(required = false) String addressId) {
        ProductSku sku = skuMapper.selectById(skuId);
        if (sku == null) throw new BizException("404", "SKU不存在");
        Product product = productMapper.selectById(sku.getProductId());
        if (product == null) throw new BizException("404", "商品不存在");

        PreOrderResponse resp = new PreOrderResponse();
        PreOrderResponse.ProductItem pp = new PreOrderResponse.ProductItem();
        pp.setId(product.getId());
        pp.setSkuId(skuId);
        pp.setName(product.getName());
        pp.setAttrsText(extractAttrsText(sku.getSpecs()));
        pp.setCount(count);
        pp.setPrice(sku.getPrice());
        pp.setPayPrice(sku.getPrice());
        pp.setPicture(imageUrlUtil.fullUrl(sku.getPicture()));
        pp.setTotalPrice(sku.getPrice().multiply(BigDecimal.valueOf(count)));
        pp.setTotalPayPrice(sku.getPrice().multiply(BigDecimal.valueOf(count)));
        resp.setProducts(List.of(pp));

        PreOrderResponse.Summary summary = new PreOrderResponse.Summary();
        summary.setTotalPrice(sku.getPrice().multiply(BigDecimal.valueOf(count)));
        summary.setPostFee(BigDecimal.ZERO);
        summary.setTotalPayPrice(sku.getPrice().multiply(BigDecimal.valueOf(count)));
        resp.setSummary(summary);

        List<Receiver> receivers = receiverMapper.selectList(new LambdaQueryWrapper<>());
        resp.setUserAddresses(receivers.stream().map(r -> {
            AddressResponse ar = new AddressResponse();
            ar.setId(r.getId());
            ar.setReceiver(r.getReceiver());
            ar.setContact(r.getContact());
            ar.setProvinceCode(r.getProvinceCode());
            ar.setCityCode(r.getCityCode());
            ar.setCountyCode(r.getCountyCode());
            ar.setAddress(r.getAddress());
            ar.setIsDefault(r.getIsDefault());
            ar.setFullLocation(r.getProvinceCode() + " " + r.getCityCode() + " " + r.getCountyCode());
            return ar;
        }).collect(Collectors.toList()));

        return Result.ok(resp);
    }

    @Operation(summary = "再次购买预付单")
    @GetMapping("/repurchase/{id}")
    public Result<PreOrderResponse> repurchase(@PathVariable String id) {
        Order order = orderMapper.selectById(id);
        if (order == null) throw new BizException("404", "订单不存在");

        List<OrderProductSku> orderSkus = orderProductSkuMapper.selectList(
                new LambdaQueryWrapper<OrderProductSku>().eq(OrderProductSku::getOrderId, id));
        if (orderSkus.isEmpty() && orderProductMapper.selectCount(
                new LambdaQueryWrapper<OrderProduct>().eq(OrderProduct::getOrderId, id)) > 0) {
            // Fallback: build from order products
        }

        PreOrderResponse resp = new PreOrderResponse();
        List<PreOrderResponse.ProductItem> products = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;
        BigDecimal totalPayPrice = BigDecimal.ZERO;

        for (OrderProductSku ops : orderSkus) {
            ProductSku latestSku = skuMapper.selectById(ops.getSkuId());
            BigDecimal currentPrice = latestSku != null ? latestSku.getPrice() : ops.getPrice();
            Product product = productMapper.selectById(ops.getProductId());

            PreOrderResponse.ProductItem pp = new PreOrderResponse.ProductItem();
            pp.setId(ops.getProductId());
            pp.setSkuId(ops.getSkuId());
            pp.setName(product != null ? product.getName() : "");
            pp.setAttrsText(extractAttrsText(ops.getSpecs()));
            pp.setCount(ops.getInventory());
            pp.setPrice(ops.getPrice());
            pp.setPayPrice(currentPrice);
            pp.setPicture(imageUrlUtil.fullUrl(ops.getPicture()));
            BigDecimal itemTotal = ops.getPrice().multiply(BigDecimal.valueOf(ops.getInventory()));
            BigDecimal itemPayTotal = currentPrice.multiply(BigDecimal.valueOf(ops.getInventory()));
            pp.setTotalPrice(itemTotal);
            pp.setTotalPayPrice(itemPayTotal);
            products.add(pp);
            totalPrice = totalPrice.add(itemTotal);
            totalPayPrice = totalPayPrice.add(itemPayTotal);
        }

        PreOrderResponse.Summary summary = new PreOrderResponse.Summary();
        summary.setTotalPrice(totalPrice);
        summary.setPostFee(BigDecimal.ZERO);
        summary.setTotalPayPrice(totalPayPrice);
        resp.setSummary(summary);

        List<Receiver> receivers = receiverMapper.selectList(new LambdaQueryWrapper<>());
        resp.setUserAddresses(receivers.stream().map(r -> {
            AddressResponse ar = new AddressResponse();
            ar.setId(r.getId());
            ar.setReceiver(r.getReceiver());
            ar.setContact(r.getContact());
            ar.setProvinceCode(r.getProvinceCode());
            ar.setCityCode(r.getCityCode());
            ar.setCountyCode(r.getCountyCode());
            ar.setAddress(r.getAddress());
            ar.setIsDefault(r.getIsDefault());
            ar.setFullLocation(r.getProvinceCode() + " " + r.getCityCode() + " " + r.getCountyCode());
            return ar;
        }).collect(Collectors.toList()));
        resp.setProducts(products);

        return Result.ok(resp);
    }

    @Operation(summary = "提交订单")
    @PostMapping
    @Transactional
    public Result<MiniOrderSubmitResponse> submit(@Valid @RequestBody MiniOrderSubmitRequest request) {
        // Validate address
        Receiver receiver = receiverMapper.selectById(request.getAddressId());
        if (receiver == null) throw new BizException("400", "收货地址不存在");

        // Build order from request products
        List<Cart> selectedCarts = cartMapper.selectList(
                new LambdaQueryWrapper<Cart>().eq(Cart::getSelected, 1));

        // If request has products, use them; otherwise use cart
        BigDecimal totalMoney = BigDecimal.ZERO;
        BigDecimal payMoney = BigDecimal.ZERO;
        List<OrderProductSku> orderSkus = new ArrayList<>();
        List<OrderProduct> orderProducts = new ArrayList<>();
        Set<String> processedProductIds = new HashSet<>();

        for (MiniOrderSubmitRequest.ProductItem pi : request.getProducts()) {
            ProductSku sku = skuMapper.selectById(pi.getSkuId());
            if (sku == null) throw new BizException("400", "SKU不存在: " + pi.getSkuId());
            if (sku.getInventory() < pi.getCount()) throw new BizException("400", "库存不足: " + pi.getSkuId());

            Product product = productMapper.selectById(sku.getProductId());
            if (product == null) throw new BizException("400", "商品不存在");

            // Decrement inventory
            sku.setInventory(sku.getInventory() - pi.getCount());
            skuMapper.updateById(sku);

            BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(pi.getCount()));
            BigDecimal itemPay = sku.getPrice().multiply(BigDecimal.valueOf(pi.getCount()));
            totalMoney = totalMoney.add(itemTotal);
            payMoney = payMoney.add(itemPay);

            // Build order SKU
            OrderProductSku ops = new OrderProductSku();
            ops.setOrderId(""); // set later
            ops.setSkuId(sku.getId());
            ops.setProductId(product.getId());
            ops.setProductType(product.getProductType());
            ops.setPrice(sku.getPrice());
            ops.setOldPrice(sku.getOldPrice());
            ops.setInventory(pi.getCount());
            ops.setPicture(imageUrlUtil.fullUrl(sku.getPicture()));
            ops.setSpecs(sku.getSpecs() != null ? sku.getSpecs() : "[]");
            ops.setCreateTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
            orderSkus.add(ops);

            // Build order product (deduplicate)
            if (processedProductIds.add(product.getId())) {
                OrderProduct op = new OrderProduct();
                op.setOrderId(""); // set later
                op.setProductId(product.getId());
                op.setProductType(product.getProductType());
                op.setProductCategory(product.getProductCategory());
                op.setName(product.getName());
                op.setDesc(product.getDesc());
                op.setPrice(product.getPrice());
                op.setOldPrice(product.getOldPrice());
                op.setMainPictures(product.getMainPictures());
                op.setPicture(imageUrlUtil.fullUrl(product.getPicture()));
                op.setDetail(product.getDetail());
                op.setSort(product.getSort());
                op.setCreateTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
                orderProducts.add(op);
            }
        }

        // Create order
        Order order = new Order();
        order.setOrderType(0);
        order.setOrderStatus(1);
        order.setProductType(orderProducts.isEmpty() ? "" : orderProducts.get(0).getProductType());
        order.setTotalMoney(totalMoney);
        order.setPayMoney(payMoney);
        order.setActualPayMoney(payMoney);
        order.setDeliveryTime(request.getDeliveryTime());
        order.setPayChannel(request.getPayChannel() != null ? request.getPayChannel() : 1);
        order.setPayType(request.getPayType() != null ? request.getPayType() : 1);
        order.setCreateTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
        orderMapper.insert(order);

        // Snapshot receiver info into t_order_receiver
        OrderReceiver orderReceiver = new OrderReceiver();
        orderReceiver.setOrderId(order.getId());
        orderReceiver.setReceiver(receiver.getReceiver());
        orderReceiver.setContact(receiver.getContact());
        orderReceiver.setProvinceCode(receiver.getProvinceCode());
        orderReceiver.setCityCode(receiver.getCityCode());
        orderReceiver.setCountyCode(receiver.getCountyCode());
        orderReceiver.setAddress(receiver.getAddress());
        orderReceiver.setCreateTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
        orderReceiverMapper.insert(orderReceiver);

        // Save order products and SKUs with order ID
        for (OrderProduct op : orderProducts) {
            op.setOrderId(order.getId());
            orderProductMapper.insert(op);
        }
        for (OrderProductSku ops : orderSkus) {
            ops.setOrderId(order.getId());
            orderProductSkuMapper.insert(ops);
        }

        // Clear selected cart items
        cartMapper.delete(new LambdaQueryWrapper<Cart>().eq(Cart::getSelected, 1));

        MiniOrderSubmitResponse resp = new MiniOrderSubmitResponse();
        resp.setId(order.getId());
        return Result.ok(resp);
    }

    private MiniOrderListResponse toListResponse(Order order) {
        MiniOrderListResponse r = new MiniOrderListResponse();
        r.setId(order.getId());
        r.setOrderState(order.getOrderStatus());
        r.setTotalMoney(order.getTotalMoney());
        r.setPayMoney(order.getPayMoney());
        r.setActualPayMoney(order.getActualPayMoney());
        r.setCreateTime(DateTimeUtil.format(order.getCreateTime()));

        OrderReceiver receiver = orderReceiverMapper.selectById(order.getId());
        if (receiver != null) {
            r.setReceiverContact(receiver.getReceiver());
            r.setReceiverMobile(receiver.getContact());
            r.setReceiverAddress(receiver.getProvinceCode() + receiver.getCityCode() + receiver.getCountyCode() + receiver.getAddress());
        }

        List<OrderProductSku> skus = orderProductSkuMapper.selectList(
                new LambdaQueryWrapper<OrderProductSku>().eq(OrderProductSku::getOrderId, order.getId()));
        r.setSkus(skus.stream().map(sku -> {
            MiniOrderListResponse.SkuItem si = new MiniOrderListResponse.SkuItem();
            si.setId(sku.getSkuId());
            si.setProductId(sku.getProductId());
            si.setName(getProductName(sku.getProductId()));
            si.setAttrsText(extractAttrsText(sku.getSpecs()));
            si.setQuantity(sku.getInventory());
            si.setPrice(sku.getPrice());
            si.setOldPrice(sku.getOldPrice());
            si.setPicture(imageUrlUtil.fullUrl(sku.getPicture()));
            return si;
        }).collect(Collectors.toList()));
        r.setTotalNum(skus.stream().mapToInt(OrderProductSku::getInventory).sum());

        return r;
    }

    @SuppressWarnings("unchecked")
    private String extractAttrsText(String specsJson) {
        if (specsJson == null || specsJson.isBlank()) return "";
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, String>> list = mapper.readValue(specsJson, List.class);
            return list.stream()
                    .map(m -> m.get("name") + "：" + m.get("valueName"))
                    .collect(Collectors.joining("，"));
        } catch (Exception e) { return ""; }
    }

    private String getProductName(String productId) {
        if (productId == null) return "";
        Product p = productMapper.selectById(productId);
        return p != null ? p.getName() : "";
    }
}
