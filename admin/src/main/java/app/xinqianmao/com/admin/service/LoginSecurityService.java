package app.xinqianmao.com.admin.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Login security: captcha after 5 errors/5min, lock after 10 errors/5min for 10min.
 * Once captcha is triggered, it stays required as long as there are >=5 failures
 * in the 5-min window. After a 10-min lockout expires, all failures have aged out
 * and the counter naturally resets.
 * Tables in mypet_config: c_admin_login_error_log, c_admin_login_lock.
 */
@Slf4j
@Service
public class LoginSecurityService {

    private final DataSource dataSource;

    public LoginSecurityService(@Qualifier("configDataSource") DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** Record a failed login attempt. */
    public void recordFailure(String tenantCode, String account, String errorType, String ip) {
        String sql = "INSERT INTO c_admin_login_error_log (id, tenant_code, account, error_type, login_ip, create_time) VALUES (?,?,?,?,?,?)";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, tenantCode);
            ps.setString(3, account);
            ps.setString(4, errorType);
            ps.setString(5, ip);
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (Exception e) { log.error("recordFailure error", e); }
    }

    /** Count failures in the last N minutes for a tenant+account combination. */
    public int countRecentFailures(String tenantCode, String account, int minutes) {
        String sql = "SELECT COUNT(*) FROM c_admin_login_error_log WHERE tenant_code=? AND account=? AND create_time > ?";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tenantCode);
            ps.setString(2, account);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now().minusMinutes(minutes)));
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            log.warn("countRecentFailures error for {}/{}: {}", tenantCode, account, e.getMessage());
            return 0;
        }
    }

    /** Check if captcha is needed: >= 5 failures in last 5 minutes for the same tenant+account. */
    public boolean needCaptcha(String tenantCode, String account) {
        int count = countRecentFailures(tenantCode, account, 5);
        if (count >= 5) {
            log.info("Captcha required for {}/{} ({} failures in 5 min)", tenantCode, account, count);
        }
        return count >= 5;
    }

    /** Lock the account for 10 minutes. */
    public void lockAccount(String tenantCode, String account) {
        // Check existing lock
        String checkSql = "SELECT id FROM c_admin_login_lock WHERE tenant_code=? AND account=? AND lock_end_time > ?";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(checkSql)) {
            ps.setString(1, tenantCode);
            ps.setString(2, account);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return; // already locked
        } catch (Exception e) { /* ignore */ }

        String sql = "INSERT INTO c_admin_login_lock (id, tenant_code, account, lock_end_time, create_time) VALUES (?,?,?,?,?)";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, tenantCode);
            ps.setString(3, account);
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now().plusMinutes(10)));
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (Exception e) { log.error("lockAccount error", e); }
    }

    /** Check if account is currently locked. */
    public boolean isLocked(String tenantCode, String account) {
        String sql = "SELECT COUNT(*) FROM c_admin_login_lock WHERE tenant_code=? AND account=? AND lock_end_time > ?";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tenantCode);
            ps.setString(2, account);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (Exception e) { return false; }
    }

    /** Check if account should be locked: >= 10 failures in last 5 minutes. */
    public boolean shouldLock(String tenantCode, String account) {
        return countRecentFailures(tenantCode, account, 5) >= 10;
    }
}
