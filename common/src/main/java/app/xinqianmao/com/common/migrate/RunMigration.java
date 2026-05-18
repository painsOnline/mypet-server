/**
 * File: RunMigration.java
 * Author: system
 * Date: 2026-05-18
 *
 * Hybrid migration orchestrator. Three phases:
 *   Phase 1 — SQL setup (DDL + light data migration)
 *   Phase 2 — Java batch processing for heavy specs data
 *   Phase 3 — SQL finalize (validate, swap, indexes, cleanup)
 *
 * SQL files are loaded from classpath (packaged in JAR).
 * Run via: MigrationController URL endpoint or main() method.
 */
package app.xinqianmao.com.common.migrate;

import java.sql.*;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class RunMigration {
    private static final String MIGRATIONS_PATH = "/sql/migrations/";

    /**
     * Execute full migration on all databases.
     * @param log callback for progress/error messages
     * @return true if all phases completed
     */
    public static boolean execute(MigrationLogger log) {
        try {
            Class.forName("org.postgresql.Driver");

            // Read tenants from config DB
            List<String> bizDbs = new ArrayList<>();
            bizDbs.add("mypet_empty");
            try (Connection c = DriverManager.getConnection(
                    MigrateConfig.url(MigrateConfig.CONFIG_DB),
                    MigrateConfig.USER, MigrateConfig.PASSWORD);
                 Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery(MigrateConfig.TENANT_QUERY)) {
                while (rs.next()) bizDbs.add("mypet_" + rs.getString("code"));
            }
            log.info("Business DBs: " + bizDbs);

            // Phase 0: UUID normalization (32-char → 36-char) on all databases
            log.info("=== PHASE 0: UUID Normalization ===");
            List<String> allDbs = new ArrayList<>();
            allDbs.add(MigrateConfig.CONFIG_DB);
            allDbs.addAll(bizDbs);
            for (String db : allDbs) {
                log.info("--- " + db + " ---");
                try (Connection c = DriverManager.getConnection(
                        MigrateConfig.url(db),
                        MigrateConfig.USER, MigrateConfig.PASSWORD)) {
                    UuidNormalizer.normalize(c, log);
                }
            }

            // Phase 1: SQL setup
            log.info("=== PHASE 1: SQL Setup ===");
            runSql(log, MigrateConfig.CONFIG_DB, "config_001_security_and_migration_tables.sql");
            for (String db : bizDbs) runSql(log, db, "002a_setup.sql");

            // Phase 2: Java batch data migration
            log.info("=== PHASE 2: Java Batch Migration ===");
            for (String db : bizDbs) {
                log.info("--- " + db + " ---");
                try (Connection c = DriverManager.getConnection(
                        MigrateConfig.url(db),
                        MigrateConfig.USER, MigrateConfig.PASSWORD)) {
                    c.setAutoCommit(false);
                    BatchMigrateSpecs.migrateTable(c, "t_product_sku", "id", true, log);
                    BatchMigrateSpecs.migrateTable(c, "t_order_product_skus", "sku_id,order_no", false, log);
                    BatchMigrateSpecs.migrateTable(c, "t_cart", "id", false, log);
                } catch (Exception e) {
                    log.error("Batch migration failed for " + db + ": " + e.getMessage());
                    return false;
                }
            }

            // Phase 3: SQL finalize
            log.info("=== PHASE 3: SQL Finalize ===");
            for (String db : bizDbs) runSql(log, db, "002b_finalize.sql");

            log.info("=== ALL DONE ===");
            return true;
        } catch (Exception e) {
            log.error("Migration fatal error: " + e.getMessage());
            return false;
        }
    }

    static void runSql(MigrationLogger log, String db, String file) {
        String resourcePath = MIGRATIONS_PATH + file;
        String sql;
        try (InputStream in = RunMigration.class.getResourceAsStream(resourcePath)) {
            if (in == null) { log.warn("SKIP " + file + " (not found in classpath)"); return; }
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) { log.error("Cannot read " + file + ": " + e.getMessage()); return; }

        sql = sql.replaceAll("(?m)^\\s*--.*$", "");
        log.info("RUN " + file + " on " + db + " (" + (sql.length() / 1024) + "KB)");

        try (Connection c = DriverManager.getConnection(
                MigrateConfig.url(db), MigrateConfig.USER, MigrateConfig.PASSWORD);
             Statement st = c.createStatement()) {
            st.execute("SET lock_timeout = '30000'");
            st.execute("SET statement_timeout = '0'");
            int ok = 0, fail = 0;
            for (String part : splitSql(sql)) {
                String t = part.strip();
                if (t.isEmpty() || t.startsWith("--")) continue;
                try { st.execute(t); ok++; }
                catch (SQLException e) {
                    fail++;
                    String m = e.getMessage();
                    String prefix = t.length() > 50 ? t.substring(0, 50).replace('\n', ' ') : t.replace('\n', ' ');
                    if (!m.contains("already exists") && !m.contains("does not exist") && !m.contains("violates not-null"))
                        log.warn("ERR [" + prefix + "]: " + m.substring(0, Math.min(150, m.length())));
                }
            }
            log.info("  " + ok + " OK, " + fail + " skipped");
        } catch (Exception e) {
            log.error("FATAL " + file + ": " + e.getMessage());
        }
    }

    /** Split SQL by ; but preserve DO $$ ... END $$; blocks as single statements. */
    static List<String> splitSql(String sql) {
        List<String> result = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        boolean inDollar = false;
        for (int i = 0; i < sql.length(); i++) {
            char ch = sql.charAt(i);
            buf.append(ch);
            if (!inDollar && buf.toString().endsWith("DO $$")) inDollar = true;
            if (inDollar && buf.toString().trim().endsWith("END $$;")) {
                inDollar = false;
                result.add(buf.toString().trim());
                buf = new StringBuilder();
            } else if (!inDollar && ch == ';') {
                result.add(buf.toString().trim());
                buf = new StringBuilder();
            }
        }
        String remainder = buf.toString().trim();
        if (!remainder.isEmpty()) result.add(remainder);
        return result;
    }

    /** Command-line entry point (for local testing). */
    public static void main(String[] args) {
        MigrationLogger log = new ConsoleLogger();
        boolean ok = execute(log);
        System.exit(ok ? 0 : 1);
    }
}
