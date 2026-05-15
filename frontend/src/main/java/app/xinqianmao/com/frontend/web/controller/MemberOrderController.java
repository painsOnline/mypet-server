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
import app.xinqianmao.com.common.utils.RegionUtil;
import app.xinqianmao.com.common.utils.ImageUrlUtil;
import app.xinqianmao.com.common.utils.OrderNoUtil;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "订单", description = "用户订单管理")
@Slf4j
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
    private final ProductPropertyMapper propertyMapper;
    private final ShopMapper shopMapper;
    private final ProductSpecsMapper specsMapper;
    private final OrderProductPropertyMapper orderProductPropertyMapper;
    private final HomeController homeController;
    private final InventoryLogMapper inventoryLogMapper;
    private final ImageUrlUtil imageUrlUtil;

    @Operation(summary = "获取订单列表")
    @GetMapping
    public Result<PageResult<MiniOrderListResponse>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "5") Integer pageSize,
            @RequestParam(required = false) Integer orderState) {
        String memberId = UserContext.getRequiredUserId();
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getMemberId, memberId);
        wrapper.eq(Order::getIsDelete, 0);
        if (orderState != null && orderState != 0) {
            wrapper.eq(Order::getOrderStatus, orderState);
        }
        wrapper.orderByDesc(Order::getCreateTime);
        Page<Order> p = Page.of(page, pageSize);
        IPage<Order> orderPage = orderMapper.selectPage(p, wrapper);
        return Result.ok(PageResult.of(orderPage.convert(this::toListResponse)));
    }

    @Operation(summary = "获取订单详情")
    @GetMapping("/{orderNo}")
    public Result<MiniOrderDetailResponse> detail(@PathVariable String orderNo) {
        Order order = getOrderByOrderNo(orderNo);
        if (order == null) throw new BizException("404", "订单不存在");
        String memberId = UserContext.getRequiredUserId();
        if (!memberId.equals(order.getMemberId())) throw new BizException("404", "订单不存在");
        MiniOrderDetailResponse resp = new MiniOrderDetailResponse();
        resp.setId(order.getId());
        resp.setOrderNo(order.getOrderNo());
        resp.setOrderState(order.getOrderStatus());
        OrderStatusEnum statusEnum = OrderStatusEnum.fromCode(order.getOrderStatus());
        resp.setOrderStateDesc(statusEnum != null ? statusEnum.getDesc() : "");
        resp.setCreateTime(DateTimeUtil.format(order.getCreateTime()));
        resp.setTotalMoney(order.getTotalMoney());
        resp.setPayMoney(order.getPayMoney());
        resp.setActualPayMoney(order.getActualPayMoney());
        resp.setDeliveryTime(DateTimeUtil.format(order.getDeliveryTime()));
        resp.setPayTime(DateTimeUtil.format(order.getPayTime()));
        resp.setReceiveTime(DateTimeUtil.format(order.getReceiveTime()));
        resp.setFinishTime(DateTimeUtil.format(order.getFinishTime()));
        resp.setCancelTime(DateTimeUtil.format(order.getCancelTime()));
        resp.setBuyerMessage(order.getBuyerMessage());

        OrderReceiver receiver = orderReceiverMapper.selectById(order.getOrderNo());
        if (receiver != null) {
            resp.setReceiverContact(receiver.getReceiver());
            resp.setReceiverMobile(receiver.getContact());
            resp.setReceiverAddress(RegionUtil.getName(receiver.getProvinceCode())
                    + RegionUtil.getName(receiver.getCityCode())
                    + RegionUtil.getName(receiver.getCountyCode())
                    + " " + receiver.getAddress());
        }

        List<OrderProductSku> skus = orderProductSkuMapper.selectList(
                new LambdaQueryWrapper<OrderProductSku>().eq(OrderProductSku::getOrderNo, order.getOrderNo()));
        resp.setSkus(skus.stream().map(os -> {
            MiniOrderDetailResponse.SkuItem si = new MiniOrderDetailResponse.SkuItem();
            si.setId(os.getSkuId());
            si.setProductId(os.getProductId());
            si.setName(getProductName(os.getProductId()));
            si.setAttrsText(extractAttrsText(os.getSpecs()));
            si.setQuantity(os.getCount());
            si.setPrice(os.getPrice());
            si.setOldPrice(os.getOldPrice());
            si.setPicture(imageUrlUtil.fullUrl(os.getPicture()));
            return si;
        }).collect(Collectors.toList()));
        resp.setTotalNum(skus.stream().mapToInt(os -> os.getCount() != null ? os.getCount() : 0).sum());

        return Result.ok(resp);
    }

    @Operation(summary = "确认收货")
    @PutMapping("/{orderNo}/receipt")
    @Transactional
    public Result<MiniOrderListResponse> confirmReceipt(@PathVariable String orderNo) {
        Order order = getOrderByOrderNo(orderNo);
        if (!UserContext.getRequiredUserId().equals(order.getMemberId())) throw new BizException("404", "订单不存在");
        if (order.getOrderStatus() != 2) throw new BizException("400", "只有配送中的订单才能确认收货");
        order.setOrderStatus(3);
        order.setModifyTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
        orderMapper.updateById(order);
        return Result.ok(toListResponse(order));
    }

    @Operation(summary = "取消订单")
    @PutMapping("/{orderNo}/cancel")
    @Transactional
    public Result<MiniOrderListResponse> cancel(@PathVariable String orderNo,
                                                 @Valid @RequestBody MiniOrderCancelRequest request) {
        Order order = getOrderByOrderNo(orderNo);
        if (!UserContext.getRequiredUserId().equals(order.getMemberId())) throw new BizException("404", "订单不存在");
        if (order.getOrderStatus() != 1) throw new BizException("400", "只有待配送的订单才能取消");
        order.setOrderStatus(5);
        order.setCancelTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
        order.setModifyTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
        orderMapper.updateById(order);
        // Return inventory on cancel (non-critical, don't rollback on error)
        try {
            returnOrderInventory(orderNo);
        } catch (Exception e) {
            log.error("Failed to return inventory for order {}: {}", orderNo, e.getMessage());
        }
        return Result.ok(toListResponse(order));
    }

    @Operation(summary = "删除订单")
    @DeleteMapping("/{orderNo}")
    public Result<Boolean> delete(@PathVariable String orderNo) {
        Order order = getOrderByOrderNo(orderNo);
        if (!UserContext.getRequiredUserId().equals(order.getMemberId())) throw new BizException("404", "订单不存在");
        if (order.getOrderStatus() != 4 && order.getOrderStatus() != 5) {
            throw new BizException("400", "只有已完成或已取消的订单才能删除");
        }
        orderMapper.deleteById(order.getId());
        orderProductMapper.delete(new LambdaQueryWrapper<OrderProduct>().eq(OrderProduct::getOrderNo, order.getOrderNo()));
        orderProductSkuMapper.delete(new LambdaQueryWrapper<OrderProductSku>().eq(OrderProductSku::getOrderNo, order.getOrderNo()));
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
        List<Receiver> receivers = receiverMapper.selectList(
                new LambdaQueryWrapper<Receiver>().eq(Receiver::getMemberId, UserContext.getRequiredUserId()));
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
        pp.setPrice(sku.getOldPrice());
        pp.setPayPrice(sku.getPrice());
        pp.setPicture(imageUrlUtil.fullUrl(sku.getPicture()));
        pp.setTotalPrice(sku.getOldPrice().multiply(BigDecimal.valueOf(count)));
        pp.setTotalPayPrice(sku.getPrice().multiply(BigDecimal.valueOf(count)));
        resp.setProducts(List.of(pp));

        PreOrderResponse.Summary summary = new PreOrderResponse.Summary();
        summary.setTotalPrice(sku.getOldPrice().multiply(BigDecimal.valueOf(count)));
        summary.setPostFee(BigDecimal.ZERO);
        summary.setTotalPayPrice(sku.getPrice().multiply(BigDecimal.valueOf(count)));
        resp.setSummary(summary);

        List<Receiver> receivers = receiverMapper.selectList(
                new LambdaQueryWrapper<Receiver>().eq(Receiver::getMemberId, UserContext.getRequiredUserId()));
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
    @GetMapping("/repurchase/{orderNo}")
    public Result<PreOrderResponse> repurchase(@PathVariable String orderNo) {
        Order order = getOrderByOrderNo(orderNo);
        if (order == null) throw new BizException("404", "订单不存在");

        List<OrderProductSku> orderSkus = orderProductSkuMapper.selectList(
                new LambdaQueryWrapper<OrderProductSku>().eq(OrderProductSku::getOrderNo, orderNo));
        if (orderSkus.isEmpty() && orderProductMapper.selectCount(
                new LambdaQueryWrapper<OrderProduct>().eq(OrderProduct::getOrderNo, orderNo)) > 0) {
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
            pp.setCount(ops.getCount());
            pp.setPrice(ops.getPrice());
            pp.setPayPrice(currentPrice);
            pp.setPicture(imageUrlUtil.fullUrl(ops.getPicture()));
            BigDecimal itemTotal = ops.getPrice().multiply(BigDecimal.valueOf(ops.getCount()));
            BigDecimal itemPayTotal = currentPrice.multiply(BigDecimal.valueOf(ops.getCount()));
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

        List<Receiver> receivers = receiverMapper.selectList(
                new LambdaQueryWrapper<Receiver>().eq(Receiver::getMemberId, UserContext.getRequiredUserId()));
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
        String memberId = UserContext.getRequiredUserId();
        Receiver receiver = receiverMapper.selectById(request.getAddressId());
        if (receiver == null) throw new BizException("400", "收货地址不存在");
        if (!memberId.equals(receiver.getMemberId())) throw new BizException("400", "收货地址不属于当前用户");

        String orderNo = OrderNoUtil.generate();
        BigDecimal totalMoney = BigDecimal.ZERO;
        BigDecimal payMoney = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        List<OrderProductSku> orderSkus = new ArrayList<>();
        List<OrderProduct> orderProducts = new ArrayList<>();
        Set<String> processedProductIds = new HashSet<>();

        for (MiniOrderSubmitRequest.ProductItem pi : request.getProducts()) {
            ProductSku sku = skuMapper.selectById(pi.getSkuId());
            if (sku == null) throw new BizException("400", "SKU不存在: " + pi.getSkuId());
            if (sku.getInventory() < pi.getCount()) throw new BizException("400", "库存不足: " + pi.getSkuId());
            Product product = productMapper.selectById(sku.getProductId());
            if (product == null) throw new BizException("400", "商品不存在");

            int before = sku.getInventory(); int after = before - pi.getCount();
            sku.setInventory(after);
            skuMapper.updateById(sku);
            writeInventoryLog(sku, orderNo, "out", pi.getCount(), before, after);

            BigDecimal itemPay = sku.getPrice().multiply(BigDecimal.valueOf(pi.getCount()));
            BigDecimal itemOrig = sku.getOldPrice().multiply(BigDecimal.valueOf(pi.getCount()));
            BigDecimal costPrice = sku.getCostPrice() != null ? sku.getCostPrice() : BigDecimal.ZERO;
            BigDecimal itemCost = costPrice.multiply(BigDecimal.valueOf(pi.getCount()));
            totalMoney = totalMoney.add(itemOrig);
            payMoney = payMoney.add(itemPay);
            totalCost = totalCost.add(itemCost);

            OrderProductSku ops = new OrderProductSku();
            ops.setOrderNo(orderNo); ops.setSkuId(sku.getId()); ops.setProductId(product.getId());
            ops.setBarcode(sku.getBarcode() != null ? sku.getBarcode() : "");
            ops.setPrice(sku.getPrice());
            ops.setOldPrice(sku.getOldPrice()); ops.setCostPrice(costPrice);
            ops.setProfitMoney(itemPay.subtract(itemCost)); ops.setCount(pi.getCount());
            ops.setPicture(sku.getPicture());
            ops.setSpecs(sku.getSpecs() != null ? sku.getSpecs() : "[]");
            ops.setCreateTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
            orderSkus.add(ops);

            if (processedProductIds.add(product.getId())) {
                OrderProduct op = new OrderProduct();
                op.setOrderNo(orderNo); op.setProductId(product.getId()); op.setProductType(product.getProductType());
                op.setProductCategory(product.getProductCategory()); op.setProductBrand(product.getProductBrand());
                op.setName(product.getName()); op.setDesc(product.getDesc());
                op.setPrice(product.getPrice()); op.setOldPrice(product.getOldPrice());
                op.setMainPictures(product.getMainPictures());
                op.setPicture(product.getPicture());
                op.setDetail(product.getDetail()); op.setSort(product.getSort());
                op.setCreateTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
                orderProducts.add(op);
            }
        }

        BigDecimal profit = payMoney.subtract(totalCost);

        // 校验实付金额不低于起配金额
        List<Shop> shops = shopMapper.selectList(new LambdaQueryWrapper<>());
        if (!shops.isEmpty() && shops.get(0).getFreeShippingAmount() != null) {
            BigDecimal minAmount = shops.get(0).getFreeShippingAmount();
            if (payMoney.compareTo(minAmount) < 0) {
                throw new BizException("400", "订单金额需满" + minAmount + "元起配");
            }
        }

        Order order = new Order();
        order.setMemberId(memberId);
        order.setOrderNo(orderNo); order.setOrderType(0); order.setOrderStatus(1);
        order.setProductType(orderProducts.isEmpty() ? "" : orderProducts.get(0).getProductType());
        order.setTotalMoney(totalMoney); order.setPayMoney(payMoney);
        order.setActualPayMoney(payMoney); order.setProfitMoney(profit);
        // deliveryTime: support null/empty gracefully (was varchar, now timestamp)
        String dt = request.getDeliveryTime();
        order.setDeliveryTime(dt != null && !dt.isBlank() ? DateTimeUtil.parse(dt) : null);
        order.setBuyerMessage(request.getBuyerMessage());
        order.setPayChannel(request.getPayChannel() != null ? request.getPayChannel() : 1);
        order.setPayType(request.getPayType() != null ? request.getPayType() : 1);
        order.setCreateTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
        orderMapper.insert(order);

        OrderReceiver orderReceiver = new OrderReceiver();
        orderReceiver.setOrderNo(orderNo);
        orderReceiver.setReceiver(receiver.getReceiver());
        orderReceiver.setContact(receiver.getContact());
        orderReceiver.setProvinceCode(receiver.getProvinceCode());
        orderReceiver.setCityCode(receiver.getCityCode());
        orderReceiver.setCountyCode(receiver.getCountyCode());
        orderReceiver.setAddress(receiver.getAddress());
        orderReceiver.setCreateTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
        orderReceiverMapper.insert(orderReceiver);

        // Save order products and SKUs
        for (OrderProduct op : orderProducts) { op.setOrderNo(orderNo); orderProductMapper.insert(op); }
        for (OrderProductSku ops : orderSkus) { ops.setOrderNo(orderNo); orderProductSkuMapper.insert(ops); }

        // Save order product properties snapshot
        for (OrderProductSku ops : orderSkus) {
            Product product = productMapper.selectById(ops.getProductId());
            if (product == null) continue;
            List<ProductProperty> props = propertyMapper.selectList(
                    new LambdaQueryWrapper<ProductProperty>().eq(ProductProperty::getProductId, product.getId())
                            .eq(ProductProperty::getIsDelete, 0));
            // Resolve spec names from specsId
            Map<String, String> specNameMap = new HashMap<>();
            if (!props.isEmpty()) {
                List<String> specIds = props.stream().map(ProductProperty::getSpecsId)
                        .filter(Objects::nonNull).distinct().collect(Collectors.toList());
                if (!specIds.isEmpty()) {
                    List<ProductSpecs> specList = specsMapper.selectBatchIds(specIds);
                    specList.forEach(s -> specNameMap.put(s.getId(), s.getName()));
                }
            }
            for (ProductProperty pp : props) {
                OrderProductProperty opp = new OrderProductProperty();
                opp.setOrderNo(orderNo); opp.setPropertyId(pp.getId()); opp.setProductId(product.getId());
                opp.setName(specNameMap.getOrDefault(pp.getSpecsId(), ""));
                opp.setValueName(pp.getValueName()); opp.setSort(pp.getSort());
                opp.setCreateTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
                orderProductPropertyMapper.insert(opp);
            }
        }

        // Clear user's selected cart items
        cartMapper.delete(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getMemberId, memberId).eq(Cart::getSelected, 1));

        MiniOrderSubmitResponse resp = new MiniOrderSubmitResponse();
        resp.setId(order.getId()); resp.setOrderNo(orderNo);
        return Result.ok(resp);
    }

    private void returnOrderInventory(String orderNo) {
        List<OrderProductSku> skus = orderProductSkuMapper.selectList(
                new LambdaQueryWrapper<OrderProductSku>().eq(OrderProductSku::getOrderNo, orderNo));
        for (OrderProductSku ops : skus) {
            ProductSku sku = skuMapper.selectById(ops.getSkuId());
            if (sku == null) continue;
            int before = sku.getInventory() != null ? sku.getInventory() : 0;
            int after = before + (ops.getCount() != null ? ops.getCount() : 0);
            sku.setInventory(after);
            skuMapper.updateById(sku);
            InventoryLog log = new InventoryLog();
            log.setSkuId(sku.getId());
            log.setBarcode(sku.getBarcode());
            log.setOrderNo(orderNo);
            log.setChangeType("in");
            log.setChangeNum(ops.getCount());
            log.setBeforeInventory(before);
            log.setAfterInventory(after);
            log.setOperator("member");
            log.setCreateTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
            inventoryLogMapper.insert(log);
        }
    }

    private MiniOrderListResponse toListResponse(Order order) {
        MiniOrderListResponse r = new MiniOrderListResponse();
        r.setId(order.getId());
        r.setOrderNo(order.getOrderNo());
        r.setOrderState(order.getOrderStatus());
        r.setTotalMoney(order.getTotalMoney());
        r.setPayMoney(order.getPayMoney());
        r.setActualPayMoney(order.getActualPayMoney());
        r.setCreateTime(DateTimeUtil.format(order.getCreateTime()));
        r.setPayTime(DateTimeUtil.format(order.getPayTime()));
        r.setDeliveryTime(DateTimeUtil.format(order.getDeliveryTime()));
        r.setReceiveTime(DateTimeUtil.format(order.getReceiveTime()));
        r.setFinishTime(DateTimeUtil.format(order.getFinishTime()));
        r.setCancelTime(DateTimeUtil.format(order.getCancelTime()));

        String orderNo = order.getOrderNo();
        OrderReceiver receiver = orderReceiverMapper.selectById(orderNo);
        if (receiver != null) {
            r.setReceiverContact(receiver.getReceiver());
            r.setReceiverMobile(receiver.getContact());
            r.setReceiverAddress(RegionUtil.getName(receiver.getProvinceCode()) + RegionUtil.getName(receiver.getCityCode()) + RegionUtil.getName(receiver.getCountyCode()) + " " + receiver.getAddress());
        }

        List<OrderProductSku> skus = orderProductSkuMapper.selectList(
                new LambdaQueryWrapper<OrderProductSku>().eq(OrderProductSku::getOrderNo, orderNo));
        r.setSkus(skus.stream().map(sku -> {
            MiniOrderListResponse.SkuItem si = new MiniOrderListResponse.SkuItem();
            si.setId(sku.getSkuId());
            si.setProductId(sku.getProductId());
            si.setName(getProductName(sku.getProductId()));
            si.setAttrsText(extractAttrsText(sku.getSpecs()));
            si.setQuantity(sku.getCount());
            si.setPrice(sku.getPrice());
            si.setOldPrice(sku.getOldPrice());
            si.setPicture(imageUrlUtil.fullUrl(sku.getPicture()));
            return si;
        }).collect(Collectors.toList()));
        r.setTotalNum(skus.stream().mapToInt(OrderProductSku::getCount).sum());

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

    private Order getOrderByOrderNo(String orderNo) {
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo)
                        .eq(Order::getIsDelete, 0));
        if (orders.isEmpty()) throw new BizException("404", "订单不存在");
        return orders.get(0);
    }

    private void writeInventoryLog(ProductSku sku, String orderNo, String changeType,
                                    int changeNum, int before, int after) {
        InventoryLog log = new InventoryLog();
        log.setSkuId(sku.getId());
        log.setBarcode(sku.getBarcode());
        log.setOrderNo(orderNo);
        log.setChangeType(changeType);
        log.setChangeNum(changeNum);
        log.setBeforeInventory(before);
        log.setAfterInventory(after);
        log.setOperator(UserContext.getUserId());
        log.setCreateTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
        inventoryLogMapper.insert(log);
    }
}
