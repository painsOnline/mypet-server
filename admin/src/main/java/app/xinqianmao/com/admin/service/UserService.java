/**
 * File: UserService.java
 * Author: system
 * Date: 2026-05-03
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

import java.time.LocalDateTime;
import java.util.List;

/**
 * User management with order statistics.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final MemberMapper memberMapper;
    private final OrderMapper orderMapper;

    public IPage<UserListResponse> search(UserSearchRequest req) {
        LambdaQueryWrapper<Member> wrapper = new LambdaQueryWrapper<>();

        if (req.getPhone() != null && !req.getPhone().isBlank()) {
            wrapper.like(Member::getMobile, req.getPhone());
        }
        if (req.getCreateTimeStart() != null) {
            LocalDateTime start = DateTimeUtil.parse(req.getCreateTimeStart());
            if (start != null) wrapper.ge(Member::getCreateTime, start);
        }
        if (req.getCreateTimeEnd() != null) {
            LocalDateTime end = DateTimeUtil.parse(req.getCreateTimeEnd());
            if (end != null) wrapper.le(Member::getCreateTime, end);
        }

        String sortBy = req.getSortBy() != null ? req.getSortBy() : "createTime";
        boolean asc = "asc".equalsIgnoreCase(req.getSortOrder());
        if (asc) {
            wrapper.orderByAsc(Member::getCreateTime);
        } else {
            wrapper.orderByDesc(Member::getCreateTime);
        }

        Page<Member> page = Page.of(req.getPage(), req.getPageSize());
        IPage<Member> memberPage = memberMapper.selectPage(page, wrapper);

        return memberPage.convert(member -> {
            UserListResponse r = new UserListResponse();
            r.setId(member.getId());
            r.setMobile(member.getMobile());
            r.setNickname(member.getNickname());
            r.setAvatar(member.getAvatar());
            r.setCreateTime(DateTimeUtil.format(member.getCreateTime()));

            // Simplified order stats
            r.setOrderCount(0L);
            r.setTotalOrderAmount(java.math.BigDecimal.ZERO);
            r.setAvgOrderAmount(java.math.BigDecimal.ZERO);

            return r;
        });
    }
}
