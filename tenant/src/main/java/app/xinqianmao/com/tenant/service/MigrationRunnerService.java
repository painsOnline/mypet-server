/**
 * File: MigrationRunnerService.java
 * Author: system
 * Date: 2026-05-21
 *
 * Migration execution service for the tenant module.
 * config_ migrations → config DB; all others → empty template + all tenant DBs.
 */
package app.xinqianmao.com.tenant.service;

import app.xinqianmao.com.tenant.dao.ConfigTenantMapper;
import app.xinqianmao.com.common.entity.Tenant;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

@Slf4j
@Service
public class MigrationRunnerService {

    private static final String MIGRATIONS_PATH = "classpath:sql/migrations/*.sql";

    private final DataSource configDataSource;
    private final DataSource templateDataSource;
    private final ConfigTenantMapper tenantMapper;

    private final ConcurrentMap<String, String> runningStatus = new ConcurrentHashMap<>();

    public MigrationRunnerService(
            @Qualifier("tenantConfigDataSource") DataSource configDataSource,
            @Qualifier("tenantTemplateDataSource") DataSource templateDataSource,
            ConfigTenantMapper tenantMapper) {
        this.configDataSource = configDataSource;
        this.templateDataSource = templateDataSource;
        this.tenantMapper = tenantMapper;
    }

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
                detail.append("[empty] ").append(executeSql(templateDataSource, sql));
                // Run on all active tenant DBs
                var tenants = tenantMapper.selectList(
                        new LambdaQueryWrapper<Tenant>()
                                .eq(Tenant::getIsDisable, 0));
                for (var tenant : tenants) {
                    try (Connection tc = getTenantConnection(tenant.getCode());
                         Statement stmt = tc.createStatement()) {
                        stmt.execute("SET lock_timeout = '30000'");
                        String[] statements = sql.replaceAll("(?m)^\\s*--.*$", "").split(";(?=(?:[^']*'[^']*')*[^']*$)");
                        for (String s : statements) {
                            String t = s.trim();
                            if (!t.isEmpty() && !t.startsWith("--")) {
                                try { stmt.execute(t); } catch (SQLException e) {
                                    String m = e.getMessage();
                                    if (!m.contains("already exists") && !m.contains("does not exist")) {
                                        log.warn("Tenant {} stmt failed: {}", tenant.getCode(),
                                                m.substring(0, Math.min(120, m.length())));
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Failed to run migration on tenant {}: {}", tenant.getCode(), e.getMessage());
                        detail.append("; [tenant ").append(tenant.getCode()).append("] ERROR: ").append(e.getMessage());
                    }
                }
                detail.append("; all tenants processed");
            }

            LocalDateTime now = LocalDateTime.now();
            updateDbStatus(name, "success", detail.toString(), getExecTime(name), now);
            runningStatus.remove(name);
            log.info("Migration {} completed", name);
            return Map.of("status", "success", "result", detail.toString());
        } catch (Exception e) {
            String errDetail = e.getMessage() + "\n" + stackTrace(e);
            log.error("Migration {} failed: {}", name, errDetail);
            updateDbStatus(name, "failed", errDetail, getExecTime(name), LocalDateTime.now());
            runningStatus.remove(name);
            return Map.of("status", "failed", "result", e.getMessage());
        }
    }

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

    public void runAllAsync() {
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        java.util.concurrent.Future<?> future = executor.submit(this::runAllPending);
        executor.shutdown();
        new Thread(() -> {
            try {
                future.get(1, java.util.concurrent.TimeUnit.HOURS);
            } catch (java.util.concurrent.TimeoutException e) {
                future.cancel(true);
                for (var e2 : runningStatus.entrySet()) {
                    if ("running".equals(e2.getValue())) {
                        updateDbStatus(e2.getKey(), "failed", "Timeout", null, LocalDateTime.now());
                        runningStatus.remove(e2.getKey());
                    }
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private Connection getTenantConnection(String tenantCode) throws SQLException {
        // Read DB connection info from config DB
        String sql = "SELECT di.host, di.port, di.\"user\", di.\"password\" FROM c_database_instance di " +
                     "JOIN c_tenant t ON t.database_instance_id = di.id WHERE t.code = ?";
        try (Connection c = configDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tenantCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String host = rs.getString("host");
                    int port = rs.getInt("port");
                    String user = rs.getString("user");
                    String password = rs.getString("password");
                    // Decrypt password
                    password = app.xinqianmao.com.common.utils.CryptoUtil.decrypt(password,
                            "mypet-jwt-secret-key-2026-minimum-32chars!!");
                    String url = "jdbc:postgresql://" + host + ":" + port + "/mypet_" + tenantCode;
                    return DriverManager.getConnection(url, user, password);
                }
            }
        }
        throw new SQLException("Cannot get connection for tenant: " + tenantCode);
    }

    private String executeSql(DataSource ds, String sql) throws SQLException {
        sql = sql.replaceAll("(?m)^\\s*--.*$", "");
        String[] statements = sql.split(";(?=(?:[^']*'[^']*')*[^']*$)");
        int ok = 0, fail = 0;
        try (Connection c = ds.getConnection(); Statement stmt = c.createStatement()) {
            stmt.execute("SET lock_timeout = '30000'");
            for (String s : statements) {
                String t = s.trim();
                if (t.isEmpty() || t.startsWith("--")) continue;
                try { stmt.execute(t); ok++; } catch (SQLException e) {
                    String m = e.getMessage();
                    if (!m.contains("already exists") && !m.contains("does not exist")) {
                        log.warn("Statement failed: {}", m.substring(0, Math.min(120, m.length())));
                    }
                    fail++;
                }
            }
        }
        return ok + " OK, " + fail + " skipped";
    }

    private Resource[] scanResources() {
        try { return new PathMatchingResourcePatternResolver().getResources(MIGRATIONS_PATH); }
        catch (IOException e) { log.error("Failed to scan migration resources", e); return null; }
    }

    private Resource findResource(String name) {
        try {
            for (Resource r : new PathMatchingResourcePatternResolver().getResources(MIGRATIONS_PATH)) {
                if (name.equals(r.getFilename())) return r;
            }
        } catch (IOException e) { /* ignore */ }
        return null;
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
        } catch (SQLException e) { log.warn("loadDbStatus: {}", e.getMessage()); }
        return result;
    }

    private void updateDbStatus(String name, String status, String result, LocalDateTime execTime, LocalDateTime endTime) {
        String upsert = "INSERT INTO c_migration_log (migration_name, status, result, exec_time, exec_end_time, create_time) "
               + "VALUES (?, ?, ?, ?, ?, now()::timestamp(0)) "
               + "ON CONFLICT (migration_name) DO UPDATE SET status=?, result=?, exec_time=?, exec_end_time=?, modify_time=now()::timestamp(0)";
        try (Connection c = configDataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(upsert)) {
            ps.setString(1, name); ps.setString(2, status); ps.setString(3, result);
            ps.setTimestamp(4, execTime != null ? Timestamp.valueOf(execTime) : null);
            ps.setTimestamp(5, endTime != null ? Timestamp.valueOf(endTime) : null);
            ps.setString(6, status); ps.setString(7, result);
            ps.setTimestamp(8, execTime != null ? Timestamp.valueOf(execTime) : null);
            ps.setTimestamp(9, endTime != null ? Timestamp.valueOf(endTime) : null);
            ps.executeUpdate();
        } catch (SQLException e) { log.error("updateDbStatus failed for {}", name, e); }
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
