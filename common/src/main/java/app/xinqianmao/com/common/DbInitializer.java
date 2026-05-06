/**
 * File: DbInitializer.java
 * Author: system
 * Date: 2026-05-04
 *
 * Initializes databases (mypet_config, mypet_empty, mypet_xlong).
 * Per README #22: never drops databases; only adds/modifies in-place. Existing data is preserved.
 *
 * Run standalone: mvn exec:java -pl common -Dexec.mainClass="app.xinqianmao.com.common.DbInitializer"
 */
package app.xinqianmao.com.common;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class DbInitializer {

    private static final String HOST = "127.0.0.1";
    private static final String PORT = "1800";
    private static final String USER = "postgres";
    private static final String PASSWORD = "mypg123abc";
    private static final String BASE_URL = "jdbc:postgresql://" + HOST + ":" + PORT + "/";
    private static final String POSTGRES_URL = BASE_URL + "postgres";

    private static volatile boolean initialized = false;

    private DbInitializer() {}

    /**
     * Standalone entry point. Creates databases if missing, runs init scripts, applies migrations.
     */
    public static void main(String[] args) {
        System.out.println("==== MyPet Database Initialization ====");
        try {
            Class.forName("org.postgresql.Driver");

            // Step 1: Create databases if not exist (no drops)
            for (String dbName : List.of("mypet_config", "mypet_empty", "mypet_xlong")) {
                ensureDatabaseExists(dbName);
            }

            // Step 2: Run init scripts (CREATE TABLE IF NOT EXISTS — idempotent)
            runInitScript(BASE_URL + "mypet_config",
                    "F:/MyWorkspace/project/mypet/java/mypet-server/sql/init-config.sql");
            runInitScript(BASE_URL + "mypet_empty",
                    "F:/MyWorkspace/project/mypet/java/mypet-server/sql/init-empty.sql");
            runInitScript(BASE_URL + "mypet_xlong",
                    "F:/MyWorkspace/project/mypet/java/mypet-server/sql/init-empty.sql");

            // Step 3: Apply migrations (ALTER TABLE etc.)
            runMigrations();

            // Step 4: Verify
            try (Connection conn = DriverManager.getConnection(BASE_URL + "mypet_xlong", USER, PASSWORD)) {
                var tables = conn.getMetaData().getTables(null, null, null, new String[]{"TABLE"});
                int count = 0;
                System.out.print("[OK] Tables in mypet_xlong: ");
                while (tables.next()) {
                    if (count > 0) System.out.print(", ");
                    System.out.print(tables.getString("TABLE_NAME"));
                    count++;
                }
                System.out.println("\n[OK] Total tables: " + count);
            }

            System.out.println("==== Initialization Complete ====");
            System.out.println("  Config DB : mypet_config");
            System.out.println("  Template  : mypet_empty");
            System.out.println("  Tenant    : mypet_xlong (code: xlong)");
            System.out.println("  Admin     : admin / admin123");
            System.out.println("  (Existing data preserved)");
        } catch (Exception e) {
            System.err.println("[FATAL] " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Called by tests to ensure all databases and tables exist.
     * Idempotent — safe to call multiple times.
     */
    public static synchronized void ensureDatabases() {
        if (initialized) return;
        try {
            Class.forName("org.postgresql.Driver");

            // Create databases if missing
            for (String dbName : List.of("mypet_config", "mypet_empty", "mypet_xlong")) {
                ensureDatabaseExists(dbName);
            }

            // Run init scripts (idempotent with IF NOT EXISTS)
            runInitScript(BASE_URL + "mypet_config",
                    "F:/MyWorkspace/project/mypet/java/mypet-server/sql/init-config.sql");
            runInitScript(BASE_URL + "mypet_empty",
                    "F:/MyWorkspace/project/mypet/java/mypet-server/sql/init-empty.sql");
            runInitScript(BASE_URL + "mypet_xlong",
                    "F:/MyWorkspace/project/mypet/java/mypet-server/sql/init-empty.sql");

            // Apply migrations
            runMigrations();

            initialized = true;
            System.out.println("[DbInit] All databases initialized (existing data preserved)");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize test databases", e);
        }
    }

    // ---- Migration definitions ----
    // Add new ALTER TABLE / CREATE INDEX etc. statements here.
    // All must be idempotent (use IF NOT EXISTS / IF EXISTS where possible).

    private static void runMigrations() {
        List<String> emptyAndTenant = List.of("mypet_empty", "mypet_xlong");
        for (String dbName : emptyAndTenant) {
            executeMigration(BASE_URL + dbName, "ALTER TABLE t_product ADD COLUMN IF NOT EXISTS is_enable SMALLINT NOT NULL DEFAULT 1");
        }
    }

    // ---- Helpers ----

    private static void ensureDatabaseExists(String dbName) {
        try (Connection conn = DriverManager.getConnection(POSTGRES_URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {
            String checkSql = "SELECT 1 FROM pg_database WHERE datname = '" + dbName + "'";
            boolean exists = false;
            try (var rs = stmt.executeQuery(checkSql)) { exists = rs.next(); }
            if (!exists) {
                stmt.execute("CREATE DATABASE \"" + dbName + "\"");
                System.out.println("[OK] Created database: " + dbName);
            } else {
                System.out.println("[OK] Database exists: " + dbName);
            }
        } catch (Exception e) {
            System.err.println("[WARN] ensureDatabaseExists " + dbName + ": " + e.getMessage());
        }
    }

    private static void executeMigration(String url, String sql) {
        try (Connection conn = DriverManager.getConnection(url, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("[MIG] OK: " + sql.substring(0, Math.min(80, sql.length())) + "...");
        } catch (Exception e) {
            System.err.println("[MIG] ERROR [" + url + "]: " + e.getMessage());
        }
    }

    private static void runInitScript(String url, String scriptPath) {
        System.out.println("[DbInit] Running " + scriptPath + " on " + url);
        try (Connection conn = DriverManager.getConnection(url, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {
            String fullSql = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(scriptPath)));
            StringBuilder cleaned = new StringBuilder();
            for (String line : fullSql.split("\n")) {
                String t = line.strip();
                if (t.startsWith("--")) continue;
                cleaned.append(line).append("\n");
            }
            int success = 0;
            for (String part : cleaned.toString().split(";")) {
                String trimmed = part.strip();
                if (trimmed.isEmpty()) continue;
                if (trimmed.toUpperCase().startsWith("CREATE DATABASE")) continue;
                try {
                    stmt.execute(trimmed);
                    success++;
                } catch (Exception e) {
                    System.err.println("[DbInit] SQL error [" + url + "]: " + e.getMessage());
                }
            }
            System.out.println("[DbInit] Done " + scriptPath + ": " + success + " statements executed");
        } catch (Exception e) {
            System.err.println("[DbInit] Fatal: " + e.getMessage());
        }
    }
}
