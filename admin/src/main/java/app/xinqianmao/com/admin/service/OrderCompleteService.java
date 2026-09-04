/**
 * File: OrderCompleteService.java
 * Author: system
 * Date: 2026-06-27
 */
package app.xinqianmao.com.admin.service;

import app.xinqianmao.com.admin.common.utils.RedisDistributedLock;
import app.xinqianmao.com.admin.dao.OrderMapper;
import app.xinqianmao.com.common.auth.TenantContext;
import app.xinqianmao.com.common.utils.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Business logic for daily order auto-completion.
 * Queries all active tenants from config DB, then processes each tenant
 * in a thread pool with Redis distributed locking.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCompleteService {

    private final OrderMapper orderMapper;
    private final RedisDistributedLock redisDistributedLock;
    @Qualifier("orderCompleteExecutor")
    private final ThreadPoolTaskExecutor orderCompleteExecutor;
    @Qualifier("configDataSource")
    private final DataSource configDataSource;

    /**
     * Query all active tenants and submit a task for each to the thread pool.
     */
    public void completeOrdersForAllTenants() {
        List<String> tenantCodes = getActiveTenantCodes();
        log.info("Order auto-completion: {} active tenants to process", tenantCodes.size());
        for (String code : tenantCodes) {
            orderCompleteExecutor.submit(() -> processTenant(code));
        }
    }

    /**
     * Process a single tenant: acquire lock → heartbeat → update orders → release.
     */
    private void processTenant(String tenantCode) {
        String lockKey = "order:complete:" + tenantCode;
        String lockValue = UUID.randomUUID().toString();

        boolean acquired = tryAcquireWithRetry(lockKey, lockValue);
        if (!acquired) {
            log.warn("Failed to acquire lock for tenant '{}' after 6 retries", tenantCode);
            return;
        }
        ScheduledExecutorService heartbeat = startHeartbeat(lockKey, lockValue);
        try {
            TenantContext.set(tenantCode);
            LocalDateTime cutoff = LocalDateTime.now(DateTimeUtil.ZONE_BEIJING).minusDays(7);
            int updated = orderMapper.updateStatusToCompleted(cutoff);
            log.info("Tenant '{}': {} orders auto-completed", tenantCode, updated);
        } catch (Exception e) {
            log.error("Error processing tenant '{}'", tenantCode, e);
        } finally {
            TenantContext.clear();
            heartbeat.shutdown();
            redisDistributedLock.release(lockKey, lockValue);
        }
    }

    /**
     * Try to acquire lock up to 6 times, waiting 5 minutes between attempts.
     */
    private boolean tryAcquireWithRetry(String lockKey, String lockValue) {
        for (int i = 0; i < 6; i++) {
            if (redisDistributedLock.acquire(lockKey, lockValue, 300)) {
                return true;
            }
            if (i < 5) {
                try {
                    Thread.sleep(5 * 60 * 1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Start a heartbeat scheduler that extends the lock every 30 seconds.
     */
    private ScheduledExecutorService startHeartbeat(String lockKey, String lockValue) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "heartbeat-" + lockKey);
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(
                () -> redisDistributedLock.extend(lockKey, lockValue, 300),
                30, 30, TimeUnit.SECONDS);
        return scheduler;
    }

    /**
     * Query all enabled tenant codes from c_tenant in config DB.
     */
    private List<String> getActiveTenantCodes() {
        String sql = "SELECT code FROM c_tenant WHERE is_disable = 0";
        List<String> codes = new ArrayList<>();
        try (Connection conn = configDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                codes.add(rs.getString("code"));
            }
        } catch (Exception e) {
            log.error("Failed to query active tenants", e);
        }
        return codes;
    }
}
