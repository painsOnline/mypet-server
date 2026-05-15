/**
 * File: MigrationRunnerService.java
 * Author: system
 * Date: 2026-05-15
 */
package app.xinqianmao.com.admin.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Executes SQL migration files and tracks results in c_migration_log.
 * Migration files are bundled in the JAR at classpath:sql/migrations/.
 * Files named with "config_" prefix target the config DB; all others target the tenant DB.
 */
@Slf4j
@Service
public class MigrationRunnerService {

    private static final String MIGRATIONS_PATH = "classpath:sql/migrations/*.sql";

    private final DataSource configDataSource;
    private final DataSource tenantDataSource;
    private final DataSource templateDataSource;

    private final ConcurrentMap<String, String> runningStatus = new ConcurrentHashMap<>();

    public MigrationRunnerService(
            @Qualifier("configDataSource") DataSource configDataSource,
            DataSource tenantDataSource,
            @Qualifier("templateDataSource") DataSource templateDataSource) {
        this.configDataSource = configDataSource;
        this.tenantDataSource = tenantDataSource;
        this.templateDataSource = templateDataSource;
    }

    /** List all migration files and their current status. */
    public List<Map<String, Object>> listMigrations() {
        List<Map<String, Object>> result = new ArrayList<>();
        Resource[] resources = scanResources();
        if (resources == null) return result;

        Map<String, Map<String, Object>> dbStatus = loadDbStatus();

        for (Resource res : resources) {
            String name = res.getFilename();
            if (name == null) continue;
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", name);
            entry.put("target", name.startsWith("config_") ? "config" : "tenant");
            if (dbStatus.containsKey(name)) {
                entry.putAll(dbStatus.get(name));
            } else {
                entry.put("status", "wait");
            }
            String running = runningStatus.get(name);
            if (running != null) entry.put("status", running);
            result.add(entry);
        }
        return result;
    }

    /** Run a specific migration synchronously. */
    public Map<String, Object> runMigration(String name) {
        Resource resource = findResource(name);
        if (resource == null) {
            return Map.of("status", "failed", "result", "Migration file not found: " + name);
        }

        runningStatus.put(name, "running");
        updateDbStatus(name, "running", null, LocalDateTime.now(), null);

        try {
            String sql = resource.getContentAsString(StandardCharsets.UTF_8);
            StringBuilder detail = new StringBuilder();
            if (name.startsWith("config_")) {
                detail.append(executeSql(configDataSource, sql));
            } else {
                detail.append("[tenant] ").append(executeSql(tenantDataSource, sql));
                detail.append("; [empty] ").append(executeSql(templateDataSource, sql));
            }

            LocalDateTime now = LocalDateTime.now();
            updateDbStatus(name, "success", detail.toString(), getExecTime(name), now);
            runningStatus.remove(name);
            log.info("Migration {} completed: {}", name, detail);
            return Map.of("status", "success", "result", detail.toString());
        } catch (Exception e) {
            String errDetail = e.getMessage() + "\n" + stackTrace(e);
            log.error("Migration {} failed: {}", name, errDetail);
            updateDbStatus(name, "failed", errDetail, getExecTime(name), LocalDateTime.now());
            runningStatus.remove(name);
            return Map.of("status", "failed", "result", e.getMessage());
        }
    }

    /** Run all pending migrations synchronously. */
    public List<Map<String, Object>> runAllPending() {
        List<Map<String, Object>> results = new ArrayList<>();
        for (Map<String, Object> m : listMigrations()) {
            String status = (String) m.get("status");
            if ("wait".equals(status) || "failed".equals(status)) {
                results.add(runMigration((String) m.get("name")));
            }
        }
        return results;
    }

    /** Run all pending migrations asynchronously. Tenant context is propagated to the worker thread. */
    public void runAllAsync() {
        String tenantCode = app.xinqianmao.com.common.auth.TenantContext.get();
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        java.util.concurrent.Future<?> future = executor.submit(() -> {
            try {
                app.xinqianmao.com.common.auth.TenantContext.set(tenantCode);
                runAllPending();
            } finally {
                app.xinqianmao.com.common.auth.TenantContext.clear();
            }
        });
        executor.shutdown();

        // Timeout watchdog: 1 hour
        new Thread(() -> {
            try {
                future.get(1, java.util.concurrent.TimeUnit.HOURS);
            } catch (java.util.concurrent.TimeoutException e) {
                future.cancel(true);
                for (java.util.Map.Entry<String, String> e2 : runningStatus.entrySet()) {
                    if ("running".equals(e2.getValue())) {
                        updateDbStatus(e2.getKey(), "failed",
                                "Timeout: execution exceeded 1 hour", null, LocalDateTime.now());
                        runningStatus.remove(e2.getKey());
                    }
                }
            } catch (Exception ignored) {
                // completed normally or failed — already recorded by runMigration
            }
        }).start();
    }

    private Resource[] scanResources() {
        try {
            return new PathMatchingResourcePatternResolver().getResources(MIGRATIONS_PATH);
        } catch (IOException e) {
            log.error("Failed to scan migration resources", e);
            return null;
        }
    }

    private Resource findResource(String name) {
        try {
            Resource[] all = new PathMatchingResourcePatternResolver().getResources(MIGRATIONS_PATH);
            for (Resource r : all) {
                if (name.equals(r.getFilename())) return r;
            }
        } catch (IOException e) { /* ignore */ }
        return null;
    }

    private String executeSql(DataSource ds, String sql) throws SQLException {
        String[] statements = sql.split(";");
        StringBuilder detail = new StringBuilder();
        try (Connection c = ds.getConnection(); Statement stmt = c.createStatement()) {
            int ok = 0;
            for (int i = 0; i < statements.length; i++) {
                String trimmed = statements[i].trim();
                if (trimmed.isEmpty()) continue;
                try {
                    stmt.execute(trimmed);
                    ok++;
                } catch (SQLException e) {
                    throw new SQLException("Statement " + (i + 1) + " failed: " + e.getMessage(), e);
                }
            }
            detail.append(ok).append(" statements executed successfully");
        }
        return detail.toString();
    }

    private String stackTrace(Throwable e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        e.printStackTrace(new java.io.PrintWriter(sw));
        return sw.toString();
    }

    private Map<String, Map<String, Object>> loadDbStatus() {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        String sql = "SELECT migration_name, status, result, exec_time, exec_end_time FROM c_migration_log ORDER BY id";
        try (Connection c = configDataSource.getConnection();
             Statement stmt = c.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("status", rs.getString("status"));
                String res = rs.getString("result");
                if (res != null) entry.put("result", res);
                Timestamp t = rs.getTimestamp("exec_time");
                if (t != null) entry.put("execTime", t.toString());
                t = rs.getTimestamp("exec_end_time");
                if (t != null) entry.put("execEndTime", t.toString());
                result.put(rs.getString("migration_name"), entry);
            }
        } catch (SQLException e) {
            log.warn("loadDbStatus: {}", e.getMessage());
        }
        return result;
    }

    private void updateDbStatus(String name, String status, String result, LocalDateTime execTime, LocalDateTime endTime) {
        String upsert = "INSERT INTO c_migration_log (migration_name, status, result, exec_time, exec_end_time, create_time) "
               + "VALUES (?, ?, ?, ?, ?, now()::timestamp(0)) "
               + "ON CONFLICT (migration_name) DO UPDATE SET status=?, result=?, exec_time=?, exec_end_time=?, modify_time=now()::timestamp(0)";
        try (Connection c = configDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(upsert)) {
            ps.setString(1, name);
            ps.setString(2, status);
            ps.setString(3, result);
            ps.setTimestamp(4, execTime != null ? Timestamp.valueOf(execTime) : null);
            ps.setTimestamp(5, endTime != null ? Timestamp.valueOf(endTime) : null);
            ps.setString(6, status);
            ps.setString(7, result);
            ps.setTimestamp(8, execTime != null ? Timestamp.valueOf(execTime) : null);
            ps.setTimestamp(9, endTime != null ? Timestamp.valueOf(endTime) : null);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("updateDbStatus failed for {}", name, e);
        }
    }

    private LocalDateTime getExecTime(String name) {
        String sql = "SELECT exec_time FROM c_migration_log WHERE migration_name = ?";
        try (Connection c = configDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Timestamp ts = rs.getTimestamp("exec_time");
                if (ts != null) return ts.toLocalDateTime();
            }
        } catch (SQLException e) { /* ignore */ }
        return LocalDateTime.now();
    }
}
