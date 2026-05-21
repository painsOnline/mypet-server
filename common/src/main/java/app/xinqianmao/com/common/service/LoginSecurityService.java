/**
 * File: LoginSecurityService.java
 * Author: system
 * Date: 2026-05-21
 *
 * Generic login security service usable by both tenant management (config DB)
 * and shop admin (tenant DB) modules. Captcha required after 5 errors in 5 min,
 * account locked for 10 min after 10 errors in 5 min.
 */
package app.xinqianmao.com.common.service;

import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
public class LoginSecurityService {

    private final DataSource dataSource;
    private final boolean useTenantCode;

    /**
     * @param dataSource    JDBC DataSource to operate on
     * @param useTenantCode true for config DB (has tenant_code column), false for tenant DB
     */
    public LoginSecurityService(DataSource dataSource, boolean useTenantCode) {
        this.dataSource = dataSource;
        this.useTenantCode = useTenantCode;
    }

    public void recordFailure(String tenantCode, String account, String errorType, String ip) {
        String sql;
        if (useTenantCode) {
            sql = "INSERT INTO c_admin_login_error_log (id, tenant_code, account, error_type, login_ip, create_time) VALUES (?,?,?,?,?,?)";
        } else {
            sql = "INSERT INTO c_admin_login_error_log (id, account, error_type, login_ip, create_time) VALUES (?,?,?,?,?)";
        }
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            int idx = 1;
            ps.setString(idx++, UUID.randomUUID().toString());
            if (useTenantCode) ps.setString(idx++, tenantCode);
            ps.setString(idx++, account);
            ps.setString(idx++, errorType);
            ps.setString(idx++, ip);
            ps.setTimestamp(idx, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (Exception e) { log.error("recordFailure error", e); }
    }

    public int countRecentFailures(String tenantCode, String account, int minutes) {
        String sql;
        if (useTenantCode) {
            sql = "SELECT COUNT(*) FROM c_admin_login_error_log WHERE tenant_code=? AND account=? AND create_time > ?";
        } else {
            sql = "SELECT COUNT(*) FROM c_admin_login_error_log WHERE account=? AND create_time > ?";
        }
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            int idx = 1;
            if (useTenantCode) ps.setString(idx++, tenantCode);
            ps.setString(idx++, account);
            ps.setTimestamp(idx, Timestamp.valueOf(LocalDateTime.now().minusMinutes(minutes)));
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            log.warn("countRecentFailures error: {}", e.getMessage());
            return 0;
        }
    }

    public boolean needCaptcha(String tenantCode, String account) {
        int count = countRecentFailures(tenantCode, account, 5);
        if (count >= 5) {
            log.info("Captcha required for {}/{} ({} failures in 5 min)", tenantCode, account, count);
        }
        return count >= 5;
    }

    public void lockAccount(String tenantCode, String account) {
        String checkSql, insertSql;
        if (useTenantCode) {
            checkSql = "SELECT id FROM c_admin_login_lock WHERE tenant_code=? AND account=? AND lock_end_time > ?";
            insertSql = "INSERT INTO c_admin_login_lock (id, tenant_code, account, lock_end_time, create_time) VALUES (?,?,?,?,?)";
        } else {
            checkSql = "SELECT id FROM c_admin_login_lock WHERE account=? AND lock_end_time > ?";
            insertSql = "INSERT INTO c_admin_login_lock (id, account, lock_end_time, create_time) VALUES (?,?,?,?)";
        }

        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(checkSql)) {
            int idx = 1;
            if (useTenantCode) ps.setString(idx++, tenantCode);
            ps.setString(idx++, account);
            ps.setTimestamp(idx, Timestamp.valueOf(LocalDateTime.now()));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return;
        } catch (Exception e) { /* ignore */ }

        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(insertSql)) {
            int idx = 1;
            ps.setString(idx++, UUID.randomUUID().toString());
            if (useTenantCode) ps.setString(idx++, tenantCode);
            ps.setString(idx++, account);
            ps.setTimestamp(idx++, Timestamp.valueOf(LocalDateTime.now().plusMinutes(10)));
            ps.setTimestamp(idx, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (Exception e) { log.error("lockAccount error", e); }
    }

    public boolean isLocked(String tenantCode, String account) {
        String sql;
        if (useTenantCode) {
            sql = "SELECT COUNT(*) FROM c_admin_login_lock WHERE tenant_code=? AND account=? AND lock_end_time > ?";
        } else {
            sql = "SELECT COUNT(*) FROM c_admin_login_lock WHERE account=? AND lock_end_time > ?";
        }
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            int idx = 1;
            if (useTenantCode) ps.setString(idx++, tenantCode);
            ps.setString(idx++, account);
            ps.setTimestamp(idx, Timestamp.valueOf(LocalDateTime.now()));
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (Exception e) { return false; }
    }

    public boolean shouldLock(String tenantCode, String account) {
        return countRecentFailures(tenantCode, account, 5) >= 10;
    }
}
