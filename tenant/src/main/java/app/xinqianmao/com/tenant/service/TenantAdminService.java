/**
 * File: TenantAdminService.java
 * Author: system
 * Date: 2026-05-21
 */
package app.xinqianmao.com.tenant.service;

import app.xinqianmao.com.common.exception.BizException;
import app.xinqianmao.com.common.utils.DateTimeUtil;
import app.xinqianmao.com.common.utils.PasswordUtil;
import app.xinqianmao.com.tenant.common.entity.TenantAdmin;
import app.xinqianmao.com.tenant.common.pojo.TenantAdminSaveRequest;
import app.xinqianmao.com.tenant.dao.TenantAdminMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tenant admin account management service.
 * Manages admin accounts in the config database (t_admin table).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantAdminService {

    private final TenantAdminMapper tenantAdminMapper;
    private final PasswordUtil passwordUtil;

    /**
     * List all tenant admins with password masked to null.
     */
    public List<TenantAdmin> listAll() {
        List<TenantAdmin> admins = tenantAdminMapper.selectList(new LambdaQueryWrapper<>());
        for (TenantAdmin admin : admins) {
            admin.setPassword(null);
        }
        return admins;
    }

    /**
     * Create a new tenant admin.
     * Validates account uniqueness and encodes the password before insertion.
     */
    public TenantAdmin create(TenantAdminSaveRequest req) {
        // Check account uniqueness
        Long count = tenantAdminMapper.selectCount(
                new LambdaQueryWrapper<TenantAdmin>().eq(TenantAdmin::getAccount, req.getAccount()));
        if (count > 0) {
            throw new BizException("400", "管理员账号已存在");
        }

        TenantAdmin admin = new TenantAdmin();
        admin.setAccount(req.getAccount());
        admin.setPassword(passwordUtil.encode(req.getPassword()));
        admin.setCreateTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
        admin.setModifyTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
        tenantAdminMapper.insert(admin);

        log.info("Tenant admin created: account={}", req.getAccount());
        // Mask password before returning
        admin.setPassword(null);
        return admin;
    }

    /**
     * Delete a tenant admin by ID.
     * At least one admin must remain in the system.
     */
    public void delete(String id) {
        Long totalCount = tenantAdminMapper.selectCount(new LambdaQueryWrapper<>());
        if (totalCount <= 1) {
            throw new BizException("400", "至少保留一个管理员");
        }

        TenantAdmin admin = tenantAdminMapper.selectById(id);
        if (admin == null) {
            throw new BizException("404", "管理员不存在");
        }

        tenantAdminMapper.deleteById(id);
        log.info("Tenant admin deleted: account={}, id={}", admin.getAccount(), id);
    }

    /**
     * Change an admin's password by ID (no old password verification needed).
     * Used by super admin to reset other admins' passwords.
     */
    public void changePassword(String id, String newPassword) {
        TenantAdmin admin = tenantAdminMapper.selectById(id);
        if (admin == null) {
            throw new BizException("404", "管理员不存在");
        }
        tenantAdminMapper.update(null,
                new LambdaUpdateWrapper<TenantAdmin>()
                        .eq(TenantAdmin::getId, id)
                        .set(TenantAdmin::getPassword, passwordUtil.encode(newPassword))
                        .set(TenantAdmin::getModifyTime, LocalDateTime.now(DateTimeUtil.ZONE_BEIJING)));
        log.info("Tenant admin password changed: account={}", admin.getAccount());
    }
}
