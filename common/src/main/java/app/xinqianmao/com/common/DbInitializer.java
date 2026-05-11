/**
 * File: DbInitializer.java
 * Author: system
 * Date: 2026-05-11
 *
 * Initializes databases (mypet_config, mypet_empty, mypet_xlong).
 * Drops and recreates all databases for a clean rebuild.
 *
 * Run standalone: mvn exec:java -pl common -Dexec.mainClass="app.xinqianmao.com.common.DbInitializer"
 */
package app.xinqianmao.com.common;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;

public final class DbInitializer {

    private static final String HOST = "127.0.0.1";
    private static final String PORT = "1800";
    private static final String USER = "postgres";
    private static final String PASSWORD = "mypg123abc";
    private static final String BASE_URL = "jdbc:postgresql://" + HOST + ":" + PORT + "/";
    private static final String POSTGRES_URL = BASE_URL + "postgres";

    private DbInitializer() {}

    /**
     * Standalone entry point. Drops databases, recreates, runs init scripts.
     */
    public static void main(String[] args) {
        System.out.println("==== MyPet Database Initialization (Clean Rebuild) ====");
        try {
            Class.forName("org.postgresql.Driver");

            // Step 1: Drop databases in reverse order
            for (String dbName : List.of("mypet_xlong", "mypet_empty", "mypet_config")) {
                dropDatabase(dbName);
            }

            // Step 2: Recreate databases
            for (String dbName : List.of("mypet_config", "mypet_empty", "mypet_xlong")) {
                createDatabase(dbName);
            }

            // Step 3: Run init scripts
            runInitScript(BASE_URL + "mypet_config",
                    "F:/MyWorkspace/project/mypet/java/mypet-server/sql/init-config.sql");
            runInitScript(BASE_URL + "mypet_empty",
                    "F:/MyWorkspace/project/mypet/java/mypet-server/sql/init-empty.sql");
            runInitScript(BASE_URL + "mypet_xlong",
                    "F:/MyWorkspace/project/mypet/java/mypet-server/sql/init-empty.sql");

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
            System.out.println("  Tenant    : mypet_xlong (code: xlong, name: 鑫钱猫惠州分店)");
            System.out.println("  Admin     : admin / admin123");
        } catch (Exception e) {
            System.err.println("[FATAL] " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Called by tests to ensure all databases and tables exist.
     * Performs a clean rebuild (drops and recreates).
     */
    public static synchronized void ensureDatabases() {
        main(new String[0]);
    }

    private static void dropDatabase(String dbName) {
        try (Connection conn = DriverManager.getConnection(POSTGRES_URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP DATABASE IF EXISTS \"" + dbName + "\"");
            System.out.println("[OK] Dropped database (if existed): " + dbName);
        } catch (Exception e) {
            System.err.println("[WARN] dropDatabase " + dbName + ": " + e.getMessage());
        }
    }

    private static void createDatabase(String dbName) {
        try (Connection conn = DriverManager.getConnection(POSTGRES_URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE DATABASE \"" + dbName + "\"");
            System.out.println("[OK] Created database: " + dbName);
        } catch (Exception e) {
            System.err.println("[WARN] createDatabase " + dbName + ": " + e.getMessage());
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
