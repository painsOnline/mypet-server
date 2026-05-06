/**
 * File: MemberOrderService.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.frontend.service;

import app.xinqianmao.com.common.enums.OrderStatusEnum;
import app.xinqianmao.com.common.exception.BizException;
import app.xinqianmao.com.common.utils.DateTimeUtil;
import app.xinqianmao.com.common.utils.UUIDUtil;
import app.xinqianmao.com.frontend.common.entity.*;
import app.xinqianmao.com.frontend.common.pojo.*;
import app.xinqianmao.com.frontend.dao.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Member order service for mini-program frontend.
 * Handles order listing, detail, status transitions, pre-order, and submission.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberOrderService {

    private final OrderMapper orderMapper;
    private final OrderProductMapper orderProductMapper;
    private final OrderProductSkuMapper orderProductSkuMapper;
    private final OrderReceiverMapper orderReceiverMapper;
    private final ReceiverMapper receiverMapper;
    private final CartMapper cartMapper;
    private final ProductSkuMapper productSkuMapper;
    private final ProductMapper productMapper;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * List orders with optional status filter and pagination.
     * Sorted by order ID descending (newest first).
     */
    public IPage<MiniOrderListResponse> listOrders(String userId, Integer orderState, int page, int pageSize) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (orderState != null && orderState != 0) {
            wrapper.eq(Order::getOrderStatus, orderState);
        }
        wrapper.orderByDesc(Order::getId);

        Page<Order> orderPage = Page.of(page, pageSize);
        IPage<Order> pagedOrders = orderMapper.selectPage(orderPage, wrapper);
        List<Order> orders = pagedOrders.getRecords();

        if (orders.isEmpty()) {
            Page<MiniOrderListResponse> emptyPage = new Page<>(page, pageSize);
            emptyPage.setRecords(List.of());
            emptyPage.setTotal(0);
            return emptyPage;
        }

        // Collect order IDs for batch preloading
        Set<String> orderIds = orders.stream().map(Order::getId).collect(Collectors.toSet());

        // Preload order receivers (snapshot in t_order_receiver)
        Map<String, OrderReceiver> receiverMap;
        if (!orderIds.isEmpty()) {
            List<OrderReceiver> orderReceivers = orderReceiverMapper.selectBatchIds(orderIds);
            receiverMap = orderReceivers.stream().collect(Collectors.toMap(OrderReceiver::getOrderId, r -> r, (a, b) -> a));
        } else {
            receiverMap = new HashMap<>();
        }
        Map<String, List<OrderProductSku>> skusByOrderId;
        Map<String, Product> productMap;
        Map<String, List<OrderProduct>> orderProductsByOrderId;
        if (!orderIds.isEmpty()) {
            List<OrderProductSku> allSkus = orderProductSkuMapper.selectList(
                    new LambdaQueryWrapper<OrderProductSku>().in(OrderProductSku::getOrderId, orderIds));
            skusByOrderId = allSkus.stream().collect(Collectors.groupingBy(OrderProductSku::getOrderId));

            // Load product names for the SKUs
            Set<String> productIds = allSkus.stream().map(OrderProductSku::getProductId).filter(Objects::nonNull).collect(Collectors.toSet());
            if (!productIds.isEmpty()) {
                List<Product> products = productMapper.selectBatchIds(productIds);
                productMap = products.stream().collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));
            } else {
                productMap = new HashMap<>();
            }

            // Also load order products for name
            if (!orderIds.isEmpty()) {
                List<OrderProduct> orderProducts = orderProductMapper.selectList(
                        new LambdaQueryWrapper<OrderProduct>().in(OrderProduct::getOrderId, orderIds));
                orderProductsByOrderId = orderProducts.stream().collect(Collectors.groupingBy(OrderProduct::getOrderId));
            } else {
                orderProductsByOrderId = new HashMap<>();
            }

            return pagedOrders.convert(order -> {
                MiniOrderListResponse r = new MiniOrderListResponse();
                r.setId(order.getId());
                r.setOrderState(order.getOrderStatus());
                r.setCreateTime(DateTimeUtil.format(order.getCreateTime()));
                r.setTotalMoney(order.getTotalMoney());
                r.setPayMoney(order.getPayMoney());
                r.setActualPayMoney(order.getActualPayMoney());

                // Receiver info
                OrderReceiver receiver = receiverMap.get(order.getId());
                if (receiver != null) {
                    r.setReceiverContact(receiver.getReceiver());
                    r.setReceiverMobile(receiver.getContact());
                    r.setReceiverAddress(receiver.getProvinceCode() + receiver.getCityCode()
                            + receiver.getCountyCode() + receiver.getAddress());
                }

                // SKU items
                List<OrderProductSku> osList = skusByOrderId.getOrDefault(order.getId(), List.of());
                r.setSkus(osList.stream().map(os -> {
                    MiniOrderListResponse.SkuItem si = new MiniOrderListResponse.SkuItem();
                    si.setId(os.getSkuId());
                    si.setProductId(os.getProductId());
                    si.setName(getProductName(os.getProductId(), orderProductsByOrderId, productMap));
                    si.setAttrsText(extractAttrsText(os.getSpecs()));
                    si.setQuantity(os.getInventory());
                    si.setPrice(os.getPrice());
                    si.setOldPrice(os.getOldPrice());
                    si.setPicture(os.getPicture());
                    return si;
                }).collect(Collectors.toList()));
                r.setTotalNum(osList.stream().mapToInt(os -> os.getInventory() != null ? os.getInventory() : 0).sum());

                return r;
            });
        }

        // Fallback without SKUs
        return pagedOrders.convert(order -> {
            MiniOrderListResponse r = new MiniOrderListResponse();
            r.setId(order.getId());
            r.setOrderState(order.getOrderStatus());
            r.setCreateTime(DateTimeUtil.format(order.getCreateTime()));
            r.setTotalMoney(order.getTotalMoney());
            r.setPayMoney(order.getPayMoney());
            r.setActualPayMoney(order.getActualPayMoney());
            r.setSkus(List.of());
            r.setTotalNum(0);
            OrderReceiver receiver = receiverMap.get(order.getId());
            if (receiver != null) {
                r.setReceiverContact(receiver.getReceiver());
                r.setReceiverMobile(receiver.getContact());
                r.setReceiverAddress(receiver.getProvinceCode() + receiver.getCityCode()
                        + receiver.getCountyCode() + receiver.getAddress());
            }
            return r;
        });
    }

    /**
     * Get full order detail.
     */
    public MiniOrderDetailResponse getDetail(String orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException("404", "订单不存在");
        }

        MiniOrderDetailResponse r = new MiniOrderDetailResponse();
        r.setId(order.getId());
        r.setOrderState(order.getOrderStatus());
        OrderStatusEnum statusEnum = OrderStatusEnum.fromCode(order.getOrderStatus());
        r.setOrderStateDesc(statusEnum != null ? statusEnum.getDesc() : "");
        r.setCreateTime(DateTimeUtil.format(order.getCreateTime()));
        r.setTotalMoney(order.getTotalMoney());
        r.setPayMoney(order.getPayMoney());
        r.setActualPayMoney(order.getActualPayMoney());

        // Receiver
        OrderReceiver receiver = orderReceiverMapper.selectById(orderId);
        if (receiver != null) {
            r.setReceiverContact(receiver.getReceiver());
            r.setReceiverMobile(receiver.getContact());
            r.setReceiverAddress(receiver.getProvinceCode() + receiver.getCityCode()
                    + receiver.getCountyCode() + receiver.getAddress());
        }

        // SKUs
        List<OrderProductSku> skus = orderProductSkuMapper.selectList(
                new LambdaQueryWrapper<OrderProductSku>().eq(OrderProductSku::getOrderId, orderId));
        r.setSkus(skus.stream().map(os -> {
            MiniOrderDetailResponse.SkuItem si = new MiniOrderDetailResponse.SkuItem();
            si.setId(os.getSkuId());
            si.setProductId(os.getProductId());
            si.setName(getOrderProductName(orderId, os.getProductId()));
            si.setAttrsText(extractAttrsText(os.getSpecs()));
            si.setQuantity(os.getInventory());
            si.setPrice(os.getPrice());
            si.setOldPrice(os.getOldPrice());
            si.setPicture(os.getPicture());
            return si;
        }).collect(Collectors.toList()));
        r.setTotalNum(skus.stream().mapToInt(os -> os.getInventory() != null ? os.getInventory() : 0).sum());

        return r;
    }

    /**
     * Confirm receipt: status 2 -> 3.
     */
    @Transactional
    public void confirmReceipt(String orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException("404", "订单不存在");
        }
        if (order.getOrderStatus() != 2) {
            throw new BizException("400", "只有配送中状态的订单才能确认收货");
        }
        order.setOrderStatus(3);
        order.setModifyTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
        orderMapper.updateById(order);
    }

    /**
     * Cancel order: status 1 -> 5.
     */
    @Transactional
    public void cancel(String orderId, String cancelReason) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException("404", "订单不存在");
        }
        if (order.getOrderStatus() != 1) {
            throw new BizException("400", "只有待配送状态的订单才能取消");
        }
        order.setOrderStatus(5);
        order.setModifyTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
        orderMapper.updateById(order);
        log.info("Order {} cancelled, reason: {}", orderId, cancelReason);
    }

    /**
     * Delete order: only if status is 4 (completed) or 5 (cancelled).
     */
    @Transactional
    public void deleteOrder(String orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException("404", "订单不存在");
        }
        if (order.getOrderStatus() != 4 && order.getOrderStatus() != 5) {
            throw new BizException("400", "只有已完成或已取消的订单才能删除");
        }
        orderMapper.deleteById(orderId);
        orderProductMapper.delete(new LambdaQueryWrapper<OrderProduct>().eq(OrderProduct::getOrderId, orderId));
        orderProductSkuMapper.delete(new LambdaQueryWrapper<OrderProductSku>().eq(OrderProductSku::getOrderId, orderId));
    }

    /**
     * Pre-order from cart: read selected cart items, build PreOrderResponse.
     */
    public PreOrderResponse preOrderFromCart(String userId) {
        List<Cart> selectedItems = cartMapper.selectList(
                new LambdaQueryWrapper<Cart>().eq(Cart::getSelected, 1));
        if (selectedItems.isEmpty()) {
            throw new BizException("400", "没有选中的商品");
        }
        return buildPreOrderFromCartItems(selectedItems);
    }

    /**
     * Pre-order now: single SKU immediate purchase.
     */
    public PreOrderResponse preOrderNow(String skuId, int count, String addressId) {
        ProductSku sku = productSkuMapper.selectById(skuId);
        if (sku == null) {
            throw new BizException("404", "SKU不存在");
        }
        Product product = productMapper.selectById(sku.getProductId());
        if (product == null) {
            throw new BizException("404", "商品不存在");
        }

        PreOrderResponse.ProductItem item = new PreOrderResponse.ProductItem();
        item.setId(product.getId());
        item.setSkuId(sku.getId());
        item.setName(product.getName());
        item.setAttrsText(extractAttrsText(sku.getSpecs()));
        item.setCount(count);
        item.setPrice(product.getOldPrice());
        item.setPayPrice(sku.getPrice());
        item.setPicture(sku.getPicture() != null ? sku.getPicture() : product.getPicture());

        BigDecimal tp = (product.getOldPrice() != null ? product.getOldPrice() : BigDecimal.ZERO)
                .multiply(BigDecimal.valueOf(count));
        item.setTotalPrice(tp);
        BigDecimal tpp = (sku.getPrice() != null ? sku.getPrice() : BigDecimal.ZERO)
                .multiply(BigDecimal.valueOf(count));
        item.setTotalPayPrice(tpp);

        PreOrderResponse response = new PreOrderResponse();
        response.setProducts(List.of(item));

        PreOrderResponse.Summary summary = new PreOrderResponse.Summary();
        summary.setTotalPrice(tp);
        summary.setPostFee(BigDecimal.ZERO);
        summary.setTotalPayPrice(tpp);
        response.setSummary(summary);

        response.setUserAddresses(loadAllAddresses());
        return response;
    }

    /**
     * Pre-order repurchase: read historical order products, get latest prices.
     */
    public PreOrderResponse preOrderRepurchase(String orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException("404", "订单不存在");
        }

        List<OrderProductSku> skus = orderProductSkuMapper.selectList(
                new LambdaQueryWrapper<OrderProductSku>().eq(OrderProductSku::getOrderId, orderId));
        if (skus.isEmpty()) {
            throw new BizException("400", "订单无商品数据");
        }

        List<PreOrderResponse.ProductItem> items = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;
        BigDecimal totalPayPrice = BigDecimal.ZERO;

        for (OrderProductSku os : skus) {
            ProductSku currentSku = productSkuMapper.selectById(os.getSkuId());
            Product product = (currentSku != null)
                    ? productMapper.selectById(currentSku.getProductId())
                    : productMapper.selectById(os.getProductId());

            PreOrderResponse.ProductItem item = new PreOrderResponse.ProductItem();
            item.setId(os.getProductId());
            item.setSkuId(os.getSkuId());
            item.setName(product != null ? product.getName() : "");
            item.setAttrsText(extractAttrsText(os.getSpecs()));
            int qty = os.getInventory() != null ? os.getInventory() : 1;
            item.setCount(qty);

            BigDecimal oldP = os.getOldPrice() != null ? os.getOldPrice() : BigDecimal.ZERO;
            BigDecimal curP = (currentSku != null && currentSku.getPrice() != null)
                    ? currentSku.getPrice() : os.getPrice() != null ? os.getPrice() : BigDecimal.ZERO;

            item.setPrice(oldP);
            item.setPayPrice(curP);
            item.setPicture(os.getPicture());

            BigDecimal lineTotal = oldP.multiply(BigDecimal.valueOf(qty));
            item.setTotalPrice(lineTotal);
            BigDecimal linePay = curP.multiply(BigDecimal.valueOf(qty));
            item.setTotalPayPrice(linePay);

            totalPrice = totalPrice.add(lineTotal);
            totalPayPrice = totalPayPrice.add(linePay);
            items.add(item);
        }

        PreOrderResponse response = new PreOrderResponse();
        response.setProducts(items);

        PreOrderResponse.Summary summary = new PreOrderResponse.Summary();
        summary.setTotalPrice(totalPrice);
        summary.setPostFee(BigDecimal.ZERO);
        summary.setTotalPayPrice(totalPayPrice);
        response.setSummary(summary);

        response.setUserAddresses(loadAllAddresses());
        return response;
    }

    /**
     * Submit order: validate stock, create order + product + SKU entries, decrement inventory, clear cart.
     */
    @Transactional
    public String submitOrder(SubmitRequest req, String userId) {
        if (req.getProducts() == null || req.getProducts().isEmpty()) {
            throw new BizException("400", "商品列表不能为空");
        }

        // Validate address
        Receiver receiver = receiverMapper.selectById(req.getAddressId());
        if (receiver == null) {
            throw new BizException("400", "收货地址不存在");
        }

        // Validate all SKUs exist and have sufficient inventory
        BigDecimal totalMoney = BigDecimal.ZERO;
        BigDecimal totalPayMoney = BigDecimal.ZERO;
        String firstProductType = null;
        List<SubmitRequest.ProductItem> items = req.getProducts();

        for (SubmitRequest.ProductItem item : items) {
            ProductSku sku = productSkuMapper.selectById(item.getSkuId());
            if (sku == null) {
                throw new BizException("400", "SKU不存在: " + item.getSkuId());
            }
            if (sku.getInventory() == null || sku.getInventory() < item.getCount()) {
                throw new BizException("400", "库存不足: " + item.getSkuId());
            }
            Product product = productMapper.selectById(sku.getProductId());
            if (product == null) {
                throw new BizException("400", "商品不存在: " + sku.getProductId());
            }

            BigDecimal oldPrice = product.getOldPrice() != null ? product.getOldPrice() : BigDecimal.ZERO;
            BigDecimal price = sku.getPrice() != null ? sku.getPrice() : BigDecimal.ZERO;
            totalMoney = totalMoney.add(oldPrice.multiply(BigDecimal.valueOf(item.getCount())));
            totalPayMoney = totalPayMoney.add(price.multiply(BigDecimal.valueOf(item.getCount())));

            if (firstProductType == null) {
                firstProductType = product.getProductType();
            }
        }

        // Calculate actual pay (same as payMoney for cash-on-delivery; discount applied elsewhere if needed)
        BigDecimal actualPayMoney = totalPayMoney;

        // Create order
        Order order = new Order();
        String orderId = UUIDUtil.uuid();
        order.setId(orderId);
        order.setOrderType(1); // confirmed order
        order.setOrderStatus(1); // pending delivery
        order.setProductType(firstProductType);
        order.setTotalMoney(totalMoney);
        order.setActualPayMoney(actualPayMoney);
        order.setPayMoney(totalPayMoney);
        order.setCreateTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
        orderMapper.insert(order);

        // Snapshot receiver info into t_order_receiver
        OrderReceiver orderReceiver = new OrderReceiver();
        orderReceiver.setOrderId(orderId);
        orderReceiver.setReceiver(receiver.getReceiver());
        orderReceiver.setContact(receiver.getContact());
        orderReceiver.setProvinceCode(receiver.getProvinceCode());
        orderReceiver.setCityCode(receiver.getCityCode());
        orderReceiver.setCountyCode(receiver.getCountyCode());
        orderReceiver.setAddress(receiver.getAddress());
        orderReceiver.setCreateTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
        orderReceiverMapper.insert(orderReceiver);

        // Create order products and SKU entries, decrement inventory
        for (SubmitRequest.ProductItem item : items) {
            ProductSku sku = productSkuMapper.selectById(item.getSkuId());
            Product product = productMapper.selectById(sku.getProductId());

            // OrderProduct (snapshot)
            OrderProduct op = new OrderProduct();
            op.setOrderId(orderId);
            op.setProductId(product.getId());
            op.setProductType(product.getProductType());
            op.setProductCategory(product.getProductCategory());
            op.setName(product.getName());
            op.setDesc(product.getDesc());
            op.setPrice(product.getPrice());
            op.setOldPrice(product.getOldPrice());
            op.setMainPictures(product.getMainPictures());
            op.setPicture(product.getPicture());
            op.setDetail(product.getDetail());
            op.setSort(product.getSort());
            op.setCreateTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
            orderProductMapper.insert(op);

            // OrderProductSku (snapshot)
            OrderProductSku ops = new OrderProductSku();
            ops.setOrderId(orderId);
            ops.setSkuId(sku.getId());
            ops.setProductId(product.getId());
            ops.setProductType(sku.getProductType());
            ops.setPrice(sku.getPrice());
            ops.setOldPrice(sku.getOldPrice());
            ops.setInventory(item.getCount());
            ops.setPicture(sku.getPicture());
            ops.setSpecs(sku.getSpecs());
            ops.setCreateTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
            orderProductSkuMapper.insert(ops);

            // Decrement inventory
            sku.setInventory(sku.getInventory() - item.getCount());
            sku.setModifyTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
            productSkuMapper.updateById(sku);
        }

        // Clear selected cart items
        cartMapper.delete(new LambdaQueryWrapper<Cart>().eq(Cart::getSelected, 1));

        return orderId;
    }

    // --- Helper methods ---

    /**
     * Build PreOrderResponse from selected cart items.
     */
    private PreOrderResponse buildPreOrderFromCartItems(List<Cart> cartItems) {
        // Collect SKU IDs
        Set<String> skuIds = cartItems.stream().map(Cart::getSkuId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<String, ProductSku> skuMap = new HashMap<>();
        Map<String, Product> productMap = new HashMap<>();
        if (!skuIds.isEmpty()) {
            List<ProductSku> skus = productSkuMapper.selectBatchIds(skuIds);
            skuMap = skus.stream().collect(Collectors.toMap(ProductSku::getId, s -> s, (a, b) -> a));
            Set<String> productIds = skus.stream().map(ProductSku::getProductId).filter(Objects::nonNull).collect(Collectors.toSet());
            if (!productIds.isEmpty()) {
                List<Product> products = productMapper.selectBatchIds(productIds);
                productMap = products.stream().collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));
            }
        }

        List<PreOrderResponse.ProductItem> items = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;
        BigDecimal totalPayPrice = BigDecimal.ZERO;

        for (Cart cart : cartItems) {
            ProductSku sku = skuMap.get(cart.getSkuId());
            Product product = sku != null ? productMap.get(sku.getProductId()) : null;

            PreOrderResponse.ProductItem item = new PreOrderResponse.ProductItem();
            item.setId(sku != null ? sku.getProductId() : null);
            item.setSkuId(cart.getSkuId());
            item.setName(cart.getName());
            item.setAttrsText(extractAttrsText(cart.getSpecs()));
            int count = cart.getCount() != null ? cart.getCount() : 1;
            item.setCount(count);

            BigDecimal price = cart.getPrice() != null ? cart.getPrice() : BigDecimal.ZERO;
            BigDecimal payPrice = cart.getNowPrice() != null ? cart.getNowPrice() : BigDecimal.ZERO;
            item.setPrice(price);
            item.setPayPrice(payPrice);
            item.setPicture(cart.getPicture());

            BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(count));
            item.setTotalPrice(lineTotal);
            BigDecimal linePay = payPrice.multiply(BigDecimal.valueOf(count));
            item.setTotalPayPrice(linePay);

            totalPrice = totalPrice.add(lineTotal);
            totalPayPrice = totalPayPrice.add(linePay);
            items.add(item);
        }

        PreOrderResponse response = new PreOrderResponse();
        response.setProducts(items);

        PreOrderResponse.Summary summary = new PreOrderResponse.Summary();
        summary.setTotalPrice(totalPrice);
        summary.setPostFee(BigDecimal.ZERO);
        summary.setTotalPayPrice(totalPayPrice);
        response.setSummary(summary);

        response.setUserAddresses(loadAllAddresses());
        return response;
    }

    /**
     * Load all addresses from t_receiver.
     */
    private List<AddressResponse> loadAllAddresses() {
        List<Receiver> receivers = receiverMapper.selectList(new LambdaQueryWrapper<>());
        return receivers.stream().map(r -> {
            AddressResponse ar = new AddressResponse();
            ar.setId(r.getId());
            ar.setReceiver(r.getReceiver());
            ar.setContact(r.getContact());
            ar.setProvinceCode(r.getProvinceCode());
            ar.setCityCode(r.getCityCode());
            ar.setCountyCode(r.getCountyCode());
            ar.setAddress(r.getAddress());
            ar.setIsDefault(r.getIsDefault());
            ar.setFullLocation((r.getProvinceCode() != null ? r.getProvinceCode() : "")
                    + " " + (r.getCityCode() != null ? r.getCityCode() : "")
                    + " " + (r.getCountyCode() != null ? r.getCountyCode() : ""));
            return ar;
        }).collect(Collectors.toList());
    }

    /**
     * Extract human-readable attrs text from specs JSON.
     */
    @SuppressWarnings("unchecked")
    private String extractAttrsText(String specsJson) {
        if (specsJson == null || specsJson.isBlank()) return "";
        String trimmed = specsJson.strip();
        if (trimmed.startsWith("[")) {
            try {
                List<Map<String, String>> list = OBJECT_MAPPER.readValue(trimmed, List.class);
                return list.stream()
                        .map(m -> m.get("name") + "：" + m.get("valueName"))
                        .collect(Collectors.joining("，"));
            } catch (Exception e) {
                // fall through
            }
        }
        return specsJson;
    }

    /**
     * Get product name from order products or product table.
     */
    private String getProductName(String productId, Map<String, List<OrderProduct>> orderProductsByOrderId,
                                  Map<String, Product> productMap) {
        // Try to find from order products
        for (List<OrderProduct> ops : orderProductsByOrderId.values()) {
            for (OrderProduct op : ops) {
                if (productId.equals(op.getProductId())) {
                    return op.getName() != null ? op.getName() : "";
                }
            }
        }
        // Fallback to product table
        Product p = productMap.get(productId);
        return p != null ? p.getName() : "";
    }

    /**
     * Get product name for a specific order by looking at order products.
     */
    private String getOrderProductName(String orderId, String productId) {
        List<OrderProduct> ops = orderProductMapper.selectList(
                new LambdaQueryWrapper<OrderProduct>()
                        .eq(OrderProduct::getOrderId, orderId)
                        .eq(OrderProduct::getProductId, productId));
        if (!ops.isEmpty() && ops.get(0).getName() != null) {
            return ops.get(0).getName();
        }
        // Fallback to product table
        Product product = productMapper.selectById(productId);
        return product != null ? product.getName() : "";
    }
}
