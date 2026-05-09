/**
 * File: OrderService.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.service;

import app.xinqianmao.com.admin.common.entity.*;
import app.xinqianmao.com.admin.common.pojo.OrderDetailResponse;
import app.xinqianmao.com.admin.common.pojo.OrderListResponse;
import app.xinqianmao.com.admin.common.pojo.OrderSearchRequest;
import app.xinqianmao.com.admin.dao.*;
import app.xinqianmao.com.common.enums.OrderStatusEnum;
import app.xinqianmao.com.common.exception.BizException;
import app.xinqianmao.com.common.utils.DateTimeUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Order management: search, detail, status transitions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderProductMapper orderProductMapper;
    private final OrderProductSkuMapper orderProductSkuMapper;
    private final OrderReceiverMapper orderReceiverMapper;
    private final MemberMapper memberMapper;

    /**
     * Get pending orders (status 1 = pending delivery).
     */
    public List<OrderListResponse> getPendingOrders() {
        List<Order> orders = orderMapper.selectPendingOrders();
        return orders.stream().map(this::toListResponse).collect(Collectors.toList());
    }

    /**
     * Search orders with filters.
     */
    public IPage<OrderListResponse> search(OrderSearchRequest req) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();

        if (req.getOrderStatus() != null) {
            wrapper.eq(Order::getOrderStatus, req.getOrderStatus());
        }
        if (req.getPriceMin() != null) {
            wrapper.ge(Order::getActualPayMoney, req.getPriceMin());
        }
        if (req.getPriceMax() != null) {
            wrapper.le(Order::getActualPayMoney, req.getPriceMax());
        }
        if (req.getCreateTimeStart() != null) {
            LocalDateTime start = DateTimeUtil.parse(req.getCreateTimeStart());
            if (start != null) wrapper.ge(Order::getCreateTime, start);
        }
        if (req.getCreateTimeEnd() != null) {
            LocalDateTime end = DateTimeUtil.parse(req.getCreateTimeEnd());
            if (end != null) wrapper.le(Order::getCreateTime, end);
        }
        if (req.getUserPhone() != null && !req.getUserPhone().isBlank()) {
            // 参数化查询：用 CONCAT 拼 LIKE 避免 SQL 注入
            wrapper.apply("EXISTS (SELECT 1 FROM t_order_receiver r WHERE r.order_id = t_order.id AND r.contact LIKE CONCAT('%', {0}, '%'))",
                    req.getUserPhone().replaceAll("[%_]", "\\\\$0"));
        }

        // Sort
        String sortBy = req.getSortBy() != null ? req.getSortBy() : "createTime";
        boolean asc = "asc".equalsIgnoreCase(req.getSortOrder());
        if ("price".equals(sortBy)) {
            wrapper.orderBy(true, asc, Order::getActualPayMoney);
        } else {
            wrapper.orderByDesc(Order::getCreateTime);
        }

        Page<Order> page = Page.of(req.getPage(), req.getPageSize());
        IPage<Order> orderPage = orderMapper.selectPage(page, wrapper);
        return orderPage.convert(this::toListResponse);
    }

    /**
     * Get order detail with customer info and products.
     */
    public OrderDetailResponse getDetail(String orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BizException("404", "订单不存在");

        OrderDetailResponse r = new OrderDetailResponse();
        r.setId(order.getId());
        r.setOrderStatus(order.getOrderStatus());
        OrderStatusEnum statusEnum = OrderStatusEnum.fromCode(order.getOrderStatus());
        r.setOrderStatusDesc(statusEnum != null ? statusEnum.getDesc() : "");
        r.setTotalMoney(order.getTotalMoney());
        r.setPayMoney(order.getPayMoney());
        r.setActualPayMoney(order.getActualPayMoney());
        r.setCreateTime(DateTimeUtil.format(order.getCreateTime()));
        r.setModifyTime(DateTimeUtil.format(order.getModifyTime()));

        // Receiver info (from snapshot)
        OrderReceiver receiver = orderReceiverMapper.selectById(order.getId());
        if (receiver != null) {
            OrderDetailResponse.ReceiverInfo ri = new OrderDetailResponse.ReceiverInfo();
            ri.setId(receiver.getOrderId());
            ri.setReceiver(receiver.getReceiver());
            ri.setContact(receiver.getContact());
            ri.setAddress(receiver.getAddress());
            ri.setFullLocation(receiver.getProvinceCode() + " " + receiver.getCityCode() + " " + receiver.getCountyCode());
            r.setReceiver(ri);
        }

        // Order products and SKUs
        Map<String, List<OrderProductSku>> skusByProduct = new HashMap<>();
        List<OrderProductSku> allSkus = orderProductSkuMapper.selectList(
                new LambdaQueryWrapper<OrderProductSku>().eq(OrderProductSku::getOrderId, orderId));
        for (OrderProductSku sku : allSkus) {
            skusByProduct.computeIfAbsent(sku.getProductId(), k -> new ArrayList<>()).add(sku);
        }

        List<OrderProduct> products = orderProductMapper.selectList(
                new LambdaQueryWrapper<OrderProduct>().eq(OrderProduct::getOrderId, orderId));
        r.setProducts(products.stream().map(p -> {
            OrderDetailResponse.ProductItem pi = new OrderDetailResponse.ProductItem();
            pi.setProductId(p.getProductId());
            pi.setName(p.getName());
            pi.setPicture(p.getPicture());
            pi.setPrice(p.getPrice());
            pi.setOldPrice(p.getOldPrice());
            List<OrderProductSku> pskus = skusByProduct.getOrDefault(p.getProductId(), List.of());
            pi.setSkus(pskus.stream().map(sku -> {
                OrderDetailResponse.SkuDetail sd = new OrderDetailResponse.SkuDetail();
                sd.setSkuId(sku.getSkuId());
                sd.setPrice(sku.getPrice());
                sd.setOldPrice(sku.getOldPrice());
                sd.setQuantity(sku.getInventory());
                sd.setPicture(sku.getPicture());
                sd.setAttrsText(extractAttrsText(sku.getSpecs()));
                return sd;
            }).collect(Collectors.toList()));
            return pi;
        }).collect(Collectors.toList()));

        return r;
    }

    /**
     * Dispatch order: status 1 -> 2.
     */
    @Transactional
    public void dispatch(String orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BizException("404", "订单不存在");
        if (order.getOrderStatus() != 1) {
            throw new BizException("400", "只有待配送状态的订单才能发货");
        }
        order.setOrderStatus(2);
        order.setModifyTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
        orderMapper.updateById(order);
    }

    /**
     * Confirm receipt: status 2 -> 3.
     */
    @Transactional
    public void confirmReceipt(String orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BizException("404", "订单不存在");
        if (order.getOrderStatus() != 2) {
            throw new BizException("400", "只有配送中状态的订单才能确认收货");
        }
        order.setOrderStatus(3);
        order.setModifyTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
        orderMapper.updateById(order);
    }

    /**
     * Approve refund: status 3 -> 5.
     */
    @Transactional
    public void approveRefund(String orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BizException("404", "订单不存在");
        if (order.getOrderStatus() != 3) {
            throw new BizException("400", "只有已收货状态的订单才能处理退货退款");
        }
        order.setOrderStatus(5);
        order.setModifyTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
        orderMapper.updateById(order);
    }

    /**
     * Reject refund: status stays.
     */
    public void rejectRefund(String orderId) {
        // Simply records the rejection; status stays 3
        log.info("Refund rejected for order: {}", orderId);
    }

    /**
     * Admin cancel: any pre-completion status -> 5.
     */
    @Transactional
    public void cancel(String orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BizException("404", "订单不存在");
        if (order.getOrderStatus() == 4 || order.getOrderStatus() == 5) {
            throw new BizException("400", "当前状态无法取消");
        }
        order.setOrderStatus(5);
        order.setModifyTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
        orderMapper.updateById(order);
    }

    /**
     * Batch dispatch — only for orders with status 1 (pending delivery).
     */
    @Transactional
    public void batchDispatch(List<String> ids) {
        for (String id : ids) {
            Order order = orderMapper.selectById(id);
            if (order == null) throw new BizException("404", "订单不存在: " + id);
            if (order.getOrderStatus() != 1) throw new BizException("400", "只能对待配送订单执行发货操作");
            order.setOrderStatus(2);
            order.setModifyTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
            orderMapper.updateById(order);
        }
    }

    /**
     * Batch cancel — only for orders that are cancellable (status != 4 and != 5).
     */
    @Transactional
    public void batchCancel(List<String> ids) {
        for (String id : ids) {
            Order order = orderMapper.selectById(id);
            if (order == null) throw new BizException("404", "订单不存在: " + id);
            if (order.getOrderStatus() == 4 || order.getOrderStatus() == 5) {
                throw new BizException("400", "订单 " + id + " 当前状态无法取消");
            }
            order.setOrderStatus(5);
            order.setModifyTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
            orderMapper.updateById(order);
        }
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
        } catch (Exception e) {
            return "";
        }
    }

    private OrderListResponse toListResponse(Order order) {
        OrderListResponse r = new OrderListResponse();
        r.setId(order.getId());
        r.setOrderStatus(order.getOrderStatus());
        OrderStatusEnum statusEnum = OrderStatusEnum.fromCode(order.getOrderStatus());
        r.setOrderStatusDesc(statusEnum != null ? statusEnum.getDesc() : "");
        r.setTotalMoney(order.getTotalMoney());
        r.setPayMoney(order.getPayMoney());
        r.setActualPayMoney(order.getActualPayMoney());
        r.setCreateTime(DateTimeUtil.format(order.getCreateTime()));
        r.setModifyTime(DateTimeUtil.format(order.getModifyTime()));

        // Status-specific times (approximated via modifyTime when status matches)
        Integer st = order.getOrderStatus();
        if (st != null && st >= 2) r.setDispatchTime(DateTimeUtil.format(order.getModifyTime()));
        if (st != null && st >= 3) r.setReceiptTime(DateTimeUtil.format(order.getModifyTime()));
        if (st != null && st == 5) r.setCancelTime(DateTimeUtil.format(order.getModifyTime()));

        // Receiver info (from snapshot)
        OrderReceiver receiver = orderReceiverMapper.selectById(order.getId());
        if (receiver != null) {
            r.setReceiverName(receiver.getReceiver());
            r.setReceiverPhone(receiver.getContact());
            r.setReceiverAddress(receiver.getProvinceCode() + receiver.getCityCode() + receiver.getCountyCode() + receiver.getAddress());
            // Try to find member by matching contact/mobile
            List<Member> members = memberMapper.selectList(
                    new LambdaQueryWrapper<Member>().eq(Member::getMobile, receiver.getContact()));
            if (!members.isEmpty()) {
                Member m = members.get(0);
                r.setMemberAvatar(m.getAvatar() != null ? m.getAvatar() : "");
                r.setMemberMobile(m.getMobile());
            }
        }

        // SKU summary
        List<OrderProductSku> skus = orderProductSkuMapper.selectList(
                new LambdaQueryWrapper<OrderProductSku>().eq(OrderProductSku::getOrderId, order.getId()));
        r.setSkus(skus.stream().map(sku -> {
            OrderListResponse.SkuItem si = new OrderListResponse.SkuItem();
            si.setSkuId(sku.getSkuId());
            si.setProductId(sku.getProductId());
            si.setQuantity(sku.getInventory());
            si.setPrice(sku.getPrice());
            si.setPicture(sku.getPicture());
            si.setAttrsText(extractAttrsText(sku.getSpecs()));
            return si;
        }).collect(Collectors.toList()));
        r.setTotalNum(skus.stream().mapToInt(OrderProductSku::getInventory).sum());

        return r;
    }
}
