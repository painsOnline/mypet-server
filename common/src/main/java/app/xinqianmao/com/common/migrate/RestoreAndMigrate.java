/**
 * File: RestoreAndMigrate.java
 * Author: system
 * Date: 2026-05-18
 *
 * Restore + Migrate: restore business tables from bak_* backups, then run full migration.
 * If DB was already overwritten with production data, skip restore and just migrate.
 */
package app.xinqianmao.com.common.migrate;

import java.sql.*;
import java.util.*;

public final class RestoreAndMigrate {

    public static void main(String[] args) throws Exception {
        Class.forName("org.postgresql.Driver");

        // Read tenants
        List<String> bizDbs = new ArrayList<>();
        bizDbs.add("mypet_empty");
        try (Connection c = DriverManager.getConnection(
                MigrateConfig.url(MigrateConfig.CONFIG_DB),
                MigrateConfig.USER, MigrateConfig.PASSWORD);
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(MigrateConfig.TENANT_QUERY)) {
            while (rs.next()) bizDbs.add("mypet_" + rs.getString("code"));
        }
        System.out.println("=== Tenants: " + bizDbs + " ===");

        // Check if production data was restored (all bak_ tables exist with data)
        boolean needRestore = false;
        for (String db : bizDbs) {
            try (Connection c = DriverManager.getConnection(
                    MigrateConfig.url(db), MigrateConfig.USER, MigrateConfig.PASSWORD);
                 Statement s = c.createStatement()) {
                // Check if migration already modified tables (specs_new exists means previous run)
                ResultSet rs = s.executeQuery(
                    "SELECT EXISTS(SELECT 1 FROM information_schema.columns " +
                    "WHERE table_name='t_product_sku' AND column_name='specs_new')");
                rs.next();
                if (rs.getBoolean(1)) {
                    System.out.println("  " + db + ": has specs_new (previous migration), need restore");
                    needRestore = true;
                    break;
                }
            }
        }

        if (needRestore) {
            System.out.println("\n=== RESTORE from bak_ tables ===");
            // Find most recent bak_ suffix
            String suffix = findBackupSuffix(bizDbs.get(0));
            System.out.println("  Using backup suffix: " + suffix);

            for (String db : bizDbs) {
                System.out.println("--- " + db + " ---");
                try (Connection c = DriverManager.getConnection(
                        MigrateConfig.url(db), MigrateConfig.USER, MigrateConfig.PASSWORD);
                     Statement s = c.createStatement()) {

                    // Find all bak_ tables for this suffix
                    List<String> tables = new ArrayList<>();
                    ResultSet rs = s.executeQuery(
                        "SELECT table_name FROM information_schema.tables " +
                        "WHERE table_schema='public' AND table_name LIKE 'bak_%" + suffix + "'");
                    while (rs.next()) {
                        String bakName = rs.getString(1);
                        String realName = bakName.substring(4, bakName.length() - suffix.length());
                        tables.add(realName);
                    }
                    rs.close();

                    for (String t : tables) {
                        try {
                            s.execute("DROP TABLE IF EXISTS " + t + " CASCADE");
                            s.execute("CREATE TABLE " + t + " AS SELECT * FROM bak_" + t + suffix);
                            System.out.println("  OK " + t);
                        } catch (SQLException e) {
                            System.out.println("  WARN " + t + ": " +
                                e.getMessage().substring(0, Math.min(80, e.getMessage().length())));
                        }
                    }

                    // Restore indexes on key tables
                    try { s.execute("ALTER TABLE t_product_specs ADD PRIMARY KEY (id)"); } catch (Exception ignored) {}
                    try { s.execute("ALTER TABLE t_product_properties ADD PRIMARY KEY (id)"); } catch (Exception ignored) {}
                    try { s.execute("ALTER TABLE t_product_sku ADD PRIMARY KEY (id)"); } catch (Exception ignored) {}
                    try { s.execute("CREATE INDEX IF NOT EXISTS idx_sku_product_id ON t_product_sku(product_id)"); } catch (Exception ignored) {}
                    try { s.execute("CREATE INDEX IF NOT EXISTS idx_sku_is_delete ON t_product_sku(is_delete)"); } catch (Exception ignored) {}
                }
            }
            System.out.println("=== RESTORE DONE ===");
        } else {
            System.out.println("\n=== DB is clean (no previous migration), skip restore ===");
        }

        // Run migration
        System.out.println("\n=== MIGRATE ===");
        boolean ok = RunMigration.execute(new ConsoleLogger());
        System.out.println("\n=== " + (ok ? "ALL DONE" : "FAILED") + " ===");
    }

    private static String findBackupSuffix(String db) throws Exception {
        try (Connection c = DriverManager.getConnection(
                MigrateConfig.url(db), MigrateConfig.USER, MigrateConfig.PASSWORD);
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(
                 "SELECT table_name FROM information_schema.tables " +
                 "WHERE table_schema='public' AND table_name LIKE 'bak\\_t\\_product\\_sku\\_%'")) {
            if (rs.next()) {
                String name = rs.getString(1);
                // "bak_t_product_sku_20260518" -> extract suffix after "bak_t_product_sku"
                return name.substring("bak_t_product_sku".length());
            }
        }
        return "_20260516"; // fallback
    }
}
