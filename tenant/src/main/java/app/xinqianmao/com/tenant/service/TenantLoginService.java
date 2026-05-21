/**
 * File: TenantLoginService.java
 * Author: system
 * Date: 2026-05-21
 */
package app.xinqianmao.com.tenant.service;

import app.xinqianmao.com.common.auth.JwtUtil;
import app.xinqianmao.com.common.exception.BizException;
import app.xinqianmao.com.common.service.CaptchaService;
import app.xinqianmao.com.common.service.LoginSecurityService;
import app.xinqianmao.com.common.utils.DateTimeUtil;
import app.xinqianmao.com.common.utils.PasswordUtil;
import app.xinqianmao.com.tenant.common.entity.TenantAdmin;
import app.xinqianmao.com.tenant.common.pojo.TenantLoginRequest;
import app.xinqianmao.com.tenant.dao.TenantAdminMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tenant management login and account management service.
 * Handles authentication for the tenant management system (config DB).
 * Tenant code is always "config" since this is the tenant management system itself.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantLoginService {

    private static final String TENANT_CODE = "config";

    private final TenantAdminMapper tenantAdminMapper;
    private final JwtUtil jwtUtil;
    private final PasswordUtil passwordUtil;
    private final LoginSecurityService securityService;
    private final CaptchaService captchaService;

    /**
     * Authenticate tenant admin by account and password.
     * Returns JWT token on success.
     */
    public String login(TenantLoginRequest req) {
        String account = req.getAccount();
        String password = req.getPassword();
        String ip = "";

        // 1. Check if account is locked
        if (securityService.isLocked(TENANT_CODE, account)) {
            throw new BizException("423", "账户已被锁定，请10分钟后再试");
        }

        // 2. Check if captcha is required
        if (securityService.needCaptcha(TENANT_CODE, account)) {
            if (req.getCaptchaToken() == null || req.getCaptchaToken().isBlank()
                    || req.getCaptchaInput() == null || req.getCaptchaInput().isBlank()) {
                throw new BizException("429", "需要验证码");
            }
            if (!captchaService.validate(req.getCaptchaToken(), req.getCaptchaInput())) {
                securityService.recordFailure(TENANT_CODE, account, "CAPTCHA", ip);
                throw new BizException("401", "验证码错误");
            }
        }

        // 3. Find admin by account
        List<TenantAdmin> admins = tenantAdminMapper.selectList(
                new LambdaQueryWrapper<TenantAdmin>().eq(TenantAdmin::getAccount, account));
        if (admins.isEmpty()) {
            securityService.recordFailure(TENANT_CODE, account, "ACCOUNT", ip);
            if (securityService.shouldLock(TENANT_CODE, account)) {
                securityService.lockAccount(TENANT_CODE, account);
            }
            throw new BizException("401", "账号或密码错误");
        }
        TenantAdmin admin = admins.get(0);

        // 4. Verify password
        if (!passwordUtil.matches(password, admin.getPassword())) {
            securityService.recordFailure(TENANT_CODE, account, "PASSWORD", ip);
            if (securityService.shouldLock(TENANT_CODE, account)) {
                securityService.lockAccount(TENANT_CODE, account);
            }
            throw new BizException("401", "账号或密码错误");
        }

        // 5. Update last login time
        tenantAdminMapper.update(null,
                new LambdaUpdateWrapper<TenantAdmin>()
                        .eq(TenantAdmin::getAccount, account)
                        .set(TenantAdmin::getLastLoginTime, LocalDateTime.now(DateTimeUtil.ZONE_BEIJING)));

        // 6. Generate and return JWT token
        return jwtUtil.generateToken(account, TENANT_CODE, true);
    }

    /**
     * Get admin info by account, with password masked.
     */
    public TenantAdmin getAdminInfo(String account) {
        List<TenantAdmin> admins = tenantAdminMapper.selectList(
                new LambdaQueryWrapper<TenantAdmin>().eq(TenantAdmin::getAccount, account));
        if (admins.isEmpty()) {
            throw new BizException("404", "管理员不存在");
        }
        TenantAdmin admin = admins.get(0);
        admin.setPassword(null);
        return admin;
    }

    /**
     * Change admin password.
     * Validates the old password first, then encodes and updates the new password.
     */
    public void changePassword(String account, String oldPassword, String newPassword) {
        List<TenantAdmin> admins = tenantAdminMapper.selectList(
                new LambdaQueryWrapper<TenantAdmin>().eq(TenantAdmin::getAccount, account));
        if (admins.isEmpty()) {
            throw new BizException("404", "管理员不存在");
        }
        TenantAdmin admin = admins.get(0);

        if (!passwordUtil.matches(oldPassword, admin.getPassword())) {
            throw new BizException("400", "旧密码错误");
        }

        tenantAdminMapper.update(null,
                new LambdaUpdateWrapper<TenantAdmin>()
                        .eq(TenantAdmin::getAccount, account)
                        .set(TenantAdmin::getPassword, passwordUtil.encode(newPassword))
                        .set(TenantAdmin::getModifyTime, LocalDateTime.now(DateTimeUtil.ZONE_BEIJING)));

        log.info("Password changed for account: {}", account);
    }
}
