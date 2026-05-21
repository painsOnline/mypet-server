/**
 * File: AdminLoginController.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.web.controller;

import app.xinqianmao.com.admin.common.entity.Admin;
import app.xinqianmao.com.admin.common.pojo.AdminLoginRequest;
import app.xinqianmao.com.admin.common.pojo.AdminLoginResponse;
import app.xinqianmao.com.admin.common.pojo.ChangePasswordRequest;
import app.xinqianmao.com.admin.service.AdminLoginService;
import app.xinqianmao.com.common.service.CaptchaService;
import app.xinqianmao.com.common.annotation.NoAuth;
import app.xinqianmao.com.common.auth.UserContext;
import app.xinqianmao.com.common.result.Result;
import app.xinqianmao.com.common.utils.DateTimeUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Admin login and account management controller.
 */
@Tag(name = "管理员登录", description = "管理员登录和密码管理")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminLoginController {

    private final AdminLoginService adminLoginService;
    private final CaptchaService captchaService;

    @NoAuth
    @Operation(summary = "获取验证码图片")
    @GetMapping("/captcha")
    public Result<java.util.Map<String, String>> captcha() {
        return Result.ok(captchaService.generate());
    }

    @NoAuth
    @Operation(summary = "管理员登录", description = "使用店铺code、账号和密码登录管理后台")
    @PostMapping("/login")
    public Result<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        String token = adminLoginService.login(request.getAccount(), request.getPassword(),
                request.getCaptchaToken(), request.getCaptchaInput());
        Admin admin = adminLoginService.getAdminInfo(request.getAccount());

        AdminLoginResponse resp = new AdminLoginResponse();
        resp.setAccount(admin.getAccount());
        resp.setLastLoginTime(DateTimeUtil.format(admin.getLastLoginTime()));
        resp.setToken(token);
        return Result.ok(resp);
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        adminLoginService.changePassword(UserContext.getUserId(),
                request.getOldPassword(), request.getNewPassword());
        return Result.ok();
    }
}
