/**
 * File: RestoreAndMigrate.java
 * Author: system
 * Date: 2026-05-18
 *
 * Restore tenant business tables from bak_* backups, then run full migration.
 * Reads tenant list dynamically from c_tenant.
 */
package app.xinqianmao.com.common.migrate;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class RestoreAndMigrate {

    public static void main(String[] args) throws Exception {
        Class.forName("org.postgresql.Driver");

        // Read tenants from config
        List<String> bizDbs = new ArrayList<>();
        bizDbs.add("mypet_empty");
        try (Connection c = DriverManager.getConnection(
                MigrateConfig.URL_PREFIX + MigrateConfig.CONFIG_DB,
                MigrateConfig.USER, MigrateConfig.PASSWORD);
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(MigrateConfig.TENANT_QUERY)) {
            while (rs.next()) bizDbs.add("mypet_" + rs.getString("code"));
        }
        System.out.println("=== Tenants: " + bizDbs + " ===");

        // Restore from bak_ tables
        String suffix = MigrateConfig.BACKUP_SUFFIX;
        String[] tables = {"t_product_specs","t_product_properties","t_product_sku",
                           "t_order_product_skus","t_order_product_properties","t_cart"};
        for (String db : bizDbs) {
            System.out.println("=== RESTORE " + db + " ===");
            try (Connection c = DriverManager.getConnection(
                    MigrateConfig.URL_PREFIX + db, MigrateConfig.USER, MigrateConfig.PASSWORD);
                 Statement s = c.createStatement()) {
                for (String t : tables) {
                    try {
                        s.execute("DROP TABLE IF EXISTS " + t + " CASCADE");
                        s.execute("CREATE TABLE " + t + " AS SELECT * FROM bak_" + t + suffix);
                        // Fix CHAR column lengths from CTAS inference
                        ResultSet cols = c.getMetaData().getColumns(null, null, "bak_" + t + suffix, null);
                        while (cols.next()) {
                            if (cols.getInt("DATA_TYPE") == Types.CHAR && cols.getInt("COLUMN_SIZE") == 36) {
                                try { s.execute("ALTER TABLE " + t + " ALTER COLUMN \"" +
                                    cols.getString("COLUMN_NAME") + "\" TYPE CHAR(36)"); }
                                catch (SQLException ignored) {}
                            }
                        }
                        cols.close();
                        System.out.println("  OK " + t);
                    } catch (SQLException e) {
                        System.out.println("  WARN " + t + ": " +
                            e.getMessage().substring(0, Math.min(100, e.getMessage().length())));
                    }
                }
                s.execute("ALTER TABLE t_product_specs ADD PRIMARY KEY (id)");
                s.execute("ALTER TABLE t_product_properties ADD PRIMARY KEY (id)");
                s.execute("ALTER TABLE t_product_sku ADD PRIMARY KEY (id)");
                s.execute("CREATE INDEX IF NOT EXISTS idx_sku_product_id ON t_product_sku(product_id)");
                s.execute("CREATE INDEX IF NOT EXISTS idx_sku_is_delete ON t_product_sku(is_delete)");
                s.execute("CREATE INDEX IF NOT EXISTS idx_properties_product_id ON t_product_properties(product_id)");
                s.execute("CREATE INDEX IF NOT EXISTS idx_properties_is_delete ON t_product_properties(is_delete)");
                System.out.println("  Indexes restored");
            }
        }

        // Run migration
        System.out.println("=== MIGRATE ===");
        boolean ok = RunMigration.execute(new ConsoleLogger());
        System.out.println("=== " + (ok ? "ALL DONE" : "FAILED") + " ===");
    }
}
