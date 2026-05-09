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
import app.xinqianmao.com.common.utils.UUIDUtil;
import app.xinqianmao.com.frontend.common.entity.Member;
import app.xinqianmao.com.frontend.common.pojo.MemberLoginResponse;
import app.xinqianmao.com.frontend.common.pojo.WxLoginRequest;
import app.xinqianmao.com.frontend.dao.MemberMapper;
import app.xinqianmao.com.frontend.service.WechatService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Tag(name = "用户登录", description = "小程序用户登录接口")
@RestController
@RequestMapping("/frontend/member/login")
@RequiredArgsConstructor
public class MemberLoginController {

    private final MemberMapper memberMapper;
    private final JwtUtil jwtUtil;
    private final WechatService wechatService;

    @NoAuth
    @Operation(summary = "微信快速登录",
            description = "wx.login() code 换 openid，openid 为唯一标识完成登录/注册")
    @PostMapping("/wxMin/quick")
    public Result<MemberLoginResponse> quickLogin(@RequestBody WxLoginRequest request) {
        String openid = resolveOpenid(request.getCode());
        Member member = findOrCreateByOpenid(openid);
        return Result.ok(buildLoginResponse(member));
    }

    private String resolveOpenid(String code) {
        if (code != null && !code.isBlank()) {
            WechatService.WechatSession session = wechatService.code2Session(code);
            if (session != null && session.openid != null && !session.openid.isBlank()) {
                return session.openid;
            }
        }
        throw new RuntimeException("微信登录失败：无法获取 openid，请检查 wechat.app-id / wechat.secret 配置是否正确");
    }

    private Member findOrCreateByOpenid(String openid) {
        if (openid != null && !openid.isBlank()) {
            List<Member> byOpenid = memberMapper.selectList(
                    new LambdaQueryWrapper<Member>().eq(Member::getOpenid, openid));
            if (!byOpenid.isEmpty()) return byOpenid.get(0);
        }
        String uid = UUIDUtil.uuid().substring(0, 8);
        Member member = new Member();
        member.setId(UUIDUtil.uuid());
        member.setOpenid(openid);
        member.setAccount(uid);
        member.setMobile("");
        member.setAvatar("");
        member.setNickname("用户" + uid.substring(uid.length() - 4));
        member.setCreateTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
        memberMapper.insert(member);
        return member;
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
