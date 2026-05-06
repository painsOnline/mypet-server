/**
 * File: MemberLoginController.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.frontend.web.controller;

import app.xinqianmao.com.common.annotation.NoAuth;
import app.xinqianmao.com.common.auth.JwtUtil;
import app.xinqianmao.com.common.auth.TenantContext;
import app.xinqianmao.com.common.result.Result;
import app.xinqianmao.com.common.utils.DateTimeUtil;
import app.xinqianmao.com.frontend.common.entity.Member;
import app.xinqianmao.com.frontend.common.pojo.AccountLoginRequest;
import app.xinqianmao.com.frontend.common.pojo.MemberLoginResponse;
import app.xinqianmao.com.frontend.common.pojo.SimpleLoginRequest;
import app.xinqianmao.com.frontend.common.pojo.WxLoginRequest;
import app.xinqianmao.com.frontend.dao.MemberMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "用户登录", description = "小程序用户登录接口")
@RestController
@RequestMapping("/member/login")
@RequiredArgsConstructor
public class MemberLoginController {

    private final MemberMapper memberMapper;
    private final JwtUtil jwtUtil;

    @NoAuth
    @Operation(summary = "微信授权登录")
    @PostMapping("/wxMin")
    public Result<MemberLoginResponse> wxLogin(@RequestBody WxLoginRequest request) {
        // Dev mode: use encryptedData phone number or mock
        Member member = findOrCreateMember("13800000000");
        return Result.ok(buildLoginResponse(member));
    }

    @NoAuth
    @Operation(summary = "内测版快捷登录", description = "开发阶段直接用手机号登录")
    @PostMapping("/wxMin/simple")
    public Result<MemberLoginResponse> simpleLogin(@Valid @RequestBody SimpleLoginRequest request) {
        Member member = findOrCreateMember(request.getPhoneNumber());
        return Result.ok(buildLoginResponse(member));
    }

    @NoAuth
    @Operation(summary = "账号密码登录")
    @PostMapping
    public Result<MemberLoginResponse> accountLogin(@Valid @RequestBody AccountLoginRequest request) {
        List<Member> members = memberMapper.selectList(
                new LambdaQueryWrapper<Member>().eq(Member::getAccount, request.getAccount()));
        if (members.isEmpty()) {
            Member member = new Member();
            member.setAccount(request.getAccount());
            member.setMobile(request.getAccount());
            member.setAvatar("");
            member.setNickname("用户" + (request.getAccount().length() > 4 ? request.getAccount().substring(request.getAccount().length() - 4) : request.getAccount()));
            member.setCreateTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
            memberMapper.insert(member);
            return Result.ok(buildLoginResponse(member));
        }
        return Result.ok(buildLoginResponse(members.get(0)));
    }

    private Member findOrCreateMember(String phoneNumber) {
        List<Member> members = memberMapper.selectList(
                new LambdaQueryWrapper<Member>().eq(Member::getMobile, phoneNumber));
        if (members.isEmpty()) {
            Member member = new Member();
            member.setAccount(phoneNumber);
            member.setMobile(phoneNumber);
            member.setAvatar("");
            member.setNickname("用户" + (phoneNumber.length() > 4 ? phoneNumber.substring(phoneNumber.length() - 4) : phoneNumber));
            member.setCreateTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
            memberMapper.insert(member);
            return member;
        }
        return members.get(0);
    }

    private MemberLoginResponse buildLoginResponse(Member member) {
        MemberLoginResponse resp = new MemberLoginResponse();
        resp.setId(member.getId());
        resp.setAccount(member.getAccount());
        resp.setMobile(member.getMobile());
        resp.setAvatar(member.getAvatar());
        resp.setNickname(member.getNickname());
        resp.setToken(jwtUtil.generateToken(member.getId(), TenantContext.get(), false));
        return resp;
    }
}
