/**
 * File: TenantLoginController.java
 * Author: system
 * Date: 2026-05-21
 */
package app.xinqianmao.com.tenant.web.controller;

import app.xinqianmao.com.common.annotation.NoAuth;
import app.xinqianmao.com.common.auth.UserContext;
import app.xinqianmao.com.common.result.Result;
import app.xinqianmao.com.common.service.CaptchaService;
import app.xinqianmao.com.common.utils.DateTimeUtil;
import app.xinqianmao.com.tenant.common.entity.TenantAdmin;
import app.xinqianmao.com.tenant.common.pojo.ChangePasswordRequest;
import app.xinqianmao.com.tenant.common.pojo.TenantLoginRequest;
import app.xinqianmao.com.tenant.common.pojo.TenantLoginResponse;
import app.xinqianmao.com.tenant.service.TenantLoginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Tenant management login controller.
 * Handles authentication for the tenant management system.
 */
@Tag(name = "租户管理-登录", description = "租户管理平台登录和密码管理")
@RestController
@RequestMapping("/tenant")
@RequiredArgsConstructor
public class TenantLoginController {

    private final TenantLoginService loginService;
    private final CaptchaService captchaService;

    @NoAuth
    @Operation(summary = "获取验证码图片")
    @GetMapping("/captcha")
    public Result<Map<String, String>> captcha() {
        return Result.ok(captchaService.generate());
    }

    @NoAuth
    @Operation(summary = "租户管理登录", description = "使用账号和密码登录租户管理平台")
    @PostMapping("/login")
    public Result<TenantLoginResponse> login(@Valid @RequestBody TenantLoginRequest request) {
        String token = loginService.login(request);
        TenantAdmin admin = loginService.getAdminInfo(request.getAccount());

        TenantLoginResponse resp = new TenantLoginResponse();
        resp.setAccount(admin.getAccount());
        resp.setLastLoginTime(DateTimeUtil.format(admin.getLastLoginTime()));
        resp.setToken(token);
        return Result.ok(resp);
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        loginService.changePassword(UserContext.getUserId(),
                request.getOldPassword(), request.getNewPassword());
        return Result.ok();
    }
}
