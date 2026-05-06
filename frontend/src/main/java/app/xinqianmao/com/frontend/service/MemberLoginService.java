/**
 * File: MemberLoginService.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.frontend.service;

import app.xinqianmao.com.common.auth.JwtUtil;
import app.xinqianmao.com.common.auth.TenantContext;
import app.xinqianmao.com.common.entity.BaseEntity;
import app.xinqianmao.com.common.exception.BizException;
import app.xinqianmao.com.common.utils.DateTimeUtil;
import app.xinqianmao.com.common.utils.UUIDUtil;
import app.xinqianmao.com.frontend.common.entity.Member;
import app.xinqianmao.com.frontend.common.pojo.MemberLoginResponse;
import app.xinqianmao.com.frontend.dao.MemberMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Member login service for mini-program frontend.
 * Handles phone-number login, account login, and token generation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberLoginService {

    private final MemberMapper memberMapper;
    private final JwtUtil jwtUtil;

    /**
     * Login by phone number. Find or create a member record.
     * If the member does not exist, create one with a generated nickname.
     * Always return a new JWT token.
     */
    public MemberLoginResponse loginByPhone(String phoneNumber) {
        List<Member> members = memberMapper.selectList(
                new LambdaQueryWrapper<Member>().eq(Member::getMobile, phoneNumber));
        Member member;
        if (members.isEmpty()) {
            member = new Member();
            member.setId(UUIDUtil.uuid());
            member.setAccount(phoneNumber);
            member.setMobile(phoneNumber);
            member.setAvatar("");
            member.setNickname("用户" + phoneNumber.substring(phoneNumber.length() - 4));
            member.setCreateTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
            memberMapper.insert(member);
        } else {
            member = members.get(0);
        }

        String tenantCode = TenantContext.get();
        String token = jwtUtil.generateToken(member.getId(), tenantCode, false);
        return buildLoginResponse(member, token);
    }

    /**
     * Login by account (phone) and password. For mini-program members, no password
     * check is performed -- only account match is required. Returns JWT token on match.
     */
    public MemberLoginResponse loginByAccount(String account, String password) {
        List<Member> members = memberMapper.selectList(
                new LambdaQueryWrapper<Member>().eq(Member::getAccount, account));
        if (members.isEmpty()) {
            throw new BizException("401", "账号不存在");
        }
        Member member = members.get(0);

        String tenantCode = TenantContext.get();
        String token = jwtUtil.generateToken(member.getId(), tenantCode, false);
        return buildLoginResponse(member, token);
    }

    /**
     * Build the login response DTO from a Member entity and token.
     */
    private MemberLoginResponse buildLoginResponse(Member member, String token) {
        MemberLoginResponse resp = new MemberLoginResponse();
        resp.setId(member.getId());
        resp.setAvatar(member.getAvatar());
        resp.setAccount(member.getAccount());
        resp.setMobile(member.getMobile());
        resp.setNickname(member.getNickname());
        resp.setToken(token);
        return resp;
    }
}
