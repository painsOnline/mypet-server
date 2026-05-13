/**
 * File: DashboardService.java
 * Author: system
 * Date: 2026-05-04
 */
package app.xinqianmao.com.admin.service;

import app.xinqianmao.com.admin.common.entity.Order;
import app.xinqianmao.com.admin.common.pojo.DashboardStatsResponse;
import app.xinqianmao.com.admin.dao.MemberMapper;
import app.xinqianmao.com.admin.dao.OrderMapper;
import app.xinqianmao.com.admin.dao.ProductMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ProductMapper productMapper;
    private final OrderMapper orderMapper;
    private final MemberMapper memberMapper;

    public DashboardStatsResponse getStats() {
        DashboardStatsResponse stats = new DashboardStatsResponse();
        stats.setProductCount(productMapper.selectCount(new LambdaQueryWrapper<>()));
        stats.setOrderCount(orderMapper.selectCount(
                new LambdaQueryWrapper<Order>().eq(Order::getIsDelete, 0)));
        stats.setPendingOrderCount(orderMapper.selectCount(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderStatus, 1)
                        .eq(Order::getIsDelete, 0)));
        stats.setUserCount(memberMapper.selectCount(new LambdaQueryWrapper<>()));
        return stats;
    }
}
