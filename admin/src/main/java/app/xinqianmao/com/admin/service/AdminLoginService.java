/**
 * File: AdminLoginService.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.service;

import app.xinqianmao.com.admin.common.entity.Admin;
import app.xinqianmao.com.admin.common.entity.Shop;
import app.xinqianmao.com.admin.dao.AdminMapper;
import app.xinqianmao.com.admin.dao.ShopMapper;
import app.xinqianmao.com.common.auth.JwtUtil;
import app.xinqianmao.com.common.auth.TenantContext;
import app.xinqianmao.com.common.exception.BizException;
import app.xinqianmao.com.common.utils.DateTimeUtil;
import app.xinqianmao.com.common.utils.PasswordUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin login and account management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminLoginService {

    private final AdminMapper adminMapper;
    private final ShopMapper shopMapper;
    private final JwtUtil jwtUtil;
    private final PasswordUtil passwordUtil;
    private final LoginSecurityService securityService;
    private final CaptchaService captchaService;

    /**
     * Authenticate admin by account and password.
     * Returns JWT token on success. The admin account identifies the admin user
     * uniquely (t_admin has no id column, account is the identifier).
     */
    public String login(String account, String password, String captchaToken, String captchaInput) {
        String tenantCode = TenantContext.get();
        String ip = "";

        try {
            // Check lock (uses config DB, safe regardless of tenant validity)
            if (securityService.isLocked(tenantCode, account)) {
                throw new BizException("423", "账户已被锁定，请10分钟后再试");
            }
            // Captcha required?
            if (securityService.needCaptcha(tenantCode, account)) {
                if (captchaToken == null || captchaToken.isBlank() || captchaInput == null || captchaInput.isBlank()) {
                    throw new BizException("429", "需要验证码");
                }
                if (!captchaService.validate(captchaToken, captchaInput)) {
                    securityService.recordFailure(tenantCode, account, "CAPTCHA", ip);
                    throw new BizException("401", "验证码错误");
                }
            }

            List<Admin> admins = adminMapper.selectList(
                    new LambdaQueryWrapper<Admin>().eq(Admin::getAccount, account));
            if (admins.isEmpty()) {
                securityService.recordFailure(tenantCode, account, "ACCOUNT", ip);
                if (securityService.shouldLock(tenantCode, account)) securityService.lockAccount(tenantCode, account);
                throw new BizException("401", "账号或密码错误");
            }
            Admin admin = admins.get(0);
            if (!passwordUtil.matches(password, admin.getPassword())) {
                securityService.recordFailure(tenantCode, account, "PASSWORD", ip);
                if (securityService.shouldLock(tenantCode, account)) securityService.lockAccount(tenantCode, account);
                throw new BizException("401", "账号或密码错误");
            }
            // Update last login time
            adminMapper.update(null,
                    new LambdaUpdateWrapper<Admin>()
                            .eq(Admin::getAccount, account)
                            .set(Admin::getLastLoginTime, LocalDateTime.now(DateTimeUtil.ZONE_BEIJING)));

            return jwtUtil.generateToken(account, tenantCode, true);
        } catch (BizException e) {
            throw e;
        } catch (RuntimeException e) {
            // Tenant not found (DynamicDataSource throws RuntimeException for unknown tenants)
            if (e.getMessage() != null && e.getMessage().contains("Unknown tenant")) {
                throw new BizException("400", "租户不存在，请输入正确的租户代码");
            }
            throw e;
        }
    }

    /**
     * Change admin password.
     */
    public void changePassword(String account, String oldPassword, String newPassword) {
        List<Admin> admins = adminMapper.selectList(
                new LambdaQueryWrapper<Admin>().eq(Admin::getAccount, account));
        if (admins.isEmpty()) {
            throw new BizException("404", "管理员不存在");
        }
        Admin admin = admins.get(0);
        if (!passwordUtil.matches(oldPassword, admin.getPassword())) {
            throw new BizException("400", "旧密码错误");
        }
        adminMapper.update(null,
                new LambdaUpdateWrapper<Admin>()
                        .eq(Admin::getAccount, account)
                        .set(Admin::getPassword, passwordUtil.encode(newPassword)));
    }

    /**
     * Get admin account and last login time.
     */
    public Admin getAdminInfo(String account) {
        List<Admin> admins = adminMapper.selectList(
                new LambdaQueryWrapper<Admin>().eq(Admin::getAccount, account));
        if (admins.isEmpty()) {
            throw new BizException("404", "管理员不存在");
        }
        return admins.get(0);
    }

    /**
     * Get shop config.
     */
    public Shop getShop() {
        List<Shop> shops = shopMapper.selectList(new LambdaQueryWrapper<>());
        return shops.isEmpty() ? null : shops.get(0);
    }
}
