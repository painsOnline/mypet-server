/**
 * File: UserService.java
 * Author: system
 * Date: 2026-05-11
 */
package app.xinqianmao.com.admin.service;

import app.xinqianmao.com.admin.common.entity.Member;
import app.xinqianmao.com.admin.common.entity.Order;
import app.xinqianmao.com.admin.common.pojo.UserListResponse;
import app.xinqianmao.com.admin.common.pojo.UserSearchRequest;
import app.xinqianmao.com.admin.dao.MemberMapper;
import app.xinqianmao.com.admin.dao.OrderMapper;
import app.xinqianmao.com.common.utils.DateTimeUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UserService {

    private final MemberMapper memberMapper;
    private final OrderMapper orderMapper;

    public IPage<UserListResponse> search(UserSearchRequest req) {
        LambdaQueryWrapper<Member> wrapper = new LambdaQueryWrapper<>();
        if (req.getPhone() != null && !req.getPhone().isBlank())
            wrapper.like(Member::getMobile, req.getPhone());
        if (req.getCreateTimeStart() != null) {
            LocalDateTime start = DateTimeUtil.parse(req.getCreateTimeStart());
            if (start != null) wrapper.ge(Member::getCreateTime, start);
        }
        if (req.getCreateTimeEnd() != null) {
            LocalDateTime end = DateTimeUtil.parse(req.getCreateTimeEnd());
            if (end != null) wrapper.le(Member::getCreateTime, end);
        }
        boolean asc = "asc".equalsIgnoreCase(req.getSortOrder());
        if (asc) wrapper.orderByAsc(Member::getCreateTime);
        else wrapper.orderByDesc(Member::getCreateTime);

        Page<Member> page = Page.of(req.getPage(), req.getPageSize());
        IPage<Member> memberPage = memberMapper.selectPage(page, wrapper);

        // Load orders for all visible members
        List<String> memberIds = memberPage.getRecords().stream().map(Member::getId).toList();
        Map<String, List<Order>> ordersByMember = new HashMap<>();
        if (!memberIds.isEmpty()) {
            for (int i = 0; i < memberIds.size(); i += 500) {
                List<String> batch = memberIds.subList(i, Math.min(i + 500, memberIds.size()));
                orderMapper.selectList(new LambdaQueryWrapper<Order>().in(Order::getMemberId, batch)
                        .eq(Order::getIsDelete, 0))
                        .forEach(o -> ordersByMember.computeIfAbsent(o.getMemberId(), k -> new ArrayList<>()).add(o));
            }
        }

        return memberPage.convert(member -> {
            UserListResponse r = new UserListResponse();
            r.setId(member.getId());
            r.setMobile(member.getMobile());
            r.setNickname(member.getNickname());
            r.setAvatar(member.getAvatar());
            r.setCreateTime(DateTimeUtil.format(member.getCreateTime()));

            List<Order> orders = ordersByMember.getOrDefault(member.getId(), List.of());
            long totalCount = orders.size();
            List<Order> received = orders.stream()
                    .filter(o -> o.getOrderStatus() != null && (o.getOrderStatus() == 3 || o.getOrderStatus() == 4)).toList();
            BigDecimal totalAmount = received.stream()
                    .map(o -> o.getActualPayMoney() != null ? o.getActualPayMoney() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long receivedCount = received.size();

            r.setOrderCount(totalCount);
            r.setTotalOrderAmount(totalAmount);
            r.setAvgOrderAmount(receivedCount > 0
                    ? totalAmount.divide(BigDecimal.valueOf(receivedCount), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);
            return r;
        });
    }
}
