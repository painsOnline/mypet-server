/**
 * File: MigrationRunnerService.java
 * Author: system
 * Date: 2026-05-15
 */
package app.xinqianmao.com.admin.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

/**
 * Executes SQL migration files and tracks results in c_migration_log.
 * Supports both config-db and tenant-db targeted migrations.
 * Migration files named with "config_" prefix target the config DB;
 * all others target the current tenant DB.
 */
@Slf4j
@Service
public class MigrationRunnerService {

    private final DataSource configDataSource;
    private final DataSource tenantDataSource;
    private final String sqlDir;

    private final ConcurrentMap<String, String> runningStatus = new ConcurrentHashMap<>();

    public MigrationRunnerService(
            @Qualifier("configDataSource") DataSource configDataSource,
            DataSource tenantDataSource,
            @Value("${mypet.migration.sql-dir:sql/migrations}") String sqlDir) {
        this.configDataSource = configDataSource;
        this.tenantDataSource = tenantDataSource;
        this.sqlDir = sqlDir;
    }

    /** List all migration files and their current status. */
    public List<Map<String, Object>> listMigrations() {
        List<Map<String, Object>> result = new ArrayList<>();
        Path dir = Paths.get(sqlDir);
        if (!Files.isDirectory(dir)) return result;

        Map<String, Map<String, Object>> dbStatus = loadDbStatus();

        try (Stream<Path> files = Files.list(dir).sorted()) {
            files.filter(f -> f.getFileName().toString().endsWith(".sql")).forEach(f -> {
                String name = f.getFileName().toString();
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
            });
        } catch (IOException e) {
            log.error("Failed to list migration files", e);
        }
        return result;
    }

    /** Run a specific migration synchronously. */
    public Map<String, Object> runMigration(String name) {
        Path file = Paths.get(sqlDir, name);
        if (!Files.isRegularFile(file)) {
            return Map.of("status", "failed", "result", "Migration file not found: " + name);
        }

        runningStatus.put(name, "running");
        updateDbStatus(name, "running", null, LocalDateTime.now(), null);

        try {
            String sql = Files.readString(file);
            DataSource ds = name.startsWith("config_") ? configDataSource : tenantDataSource;
            executeSql(ds, sql);

            LocalDateTime now = LocalDateTime.now();
            updateDbStatus(name, "success", "ok", getExecTime(name), now);
            runningStatus.remove(name);
            log.info("Migration {} completed successfully", name);
            return Map.of("status", "success", "result", "ok");
        } catch (Exception e) {
            log.error("Migration {} failed", name, e);
            updateDbStatus(name, "failed", e.getMessage(), getExecTime(name), LocalDateTime.now());
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

    /** Run all pending migrations asynchronously (fire and forget). */
    public void runAllAsync() {
        new Thread(() -> runAllPending()).start();
    }

    private void executeSql(DataSource ds, String sql) throws SQLException {
        String[] statements = sql.split(";");
        try (Connection c = ds.getConnection(); Statement stmt = c.createStatement()) {
            for (String s : statements) {
                String trimmed = s.trim();
                if (trimmed.isEmpty()) continue;
                stmt.execute(trimmed);
            }
        }
    }

    private Map<String, Map<String, Object>> loadDbStatus() {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        String sql = "SELECT migration_name, migration_desc, status, result, exec_time, exec_end_time FROM c_migration_log ORDER BY id";
        try (Connection c = configDataSource.getConnection();
             Statement stmt = c.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("status", rs.getString("status"));
                String desc = rs.getString("migration_desc");
                if (desc != null) entry.put("description", desc);
                String res = rs.getString("result");
                if (res != null) entry.put("result", res);
                Timestamp execTime = rs.getTimestamp("exec_time");
                if (execTime != null) entry.put("execTime", execTime.toString());
                Timestamp endTime = rs.getTimestamp("exec_end_time");
                if (endTime != null) entry.put("execEndTime", endTime.toString());
                result.put(rs.getString("migration_name"), entry);
            }
        } catch (SQLException e) {
            log.warn("Failed to load migration status (table may not exist yet): {}", e.getMessage());
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
            log.error("Failed to update migration status for {}", name, e);
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
