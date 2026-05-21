/**
 * File: MigrationMockTest.java
 * Author: system
 * Date: 2026-05-21
 *
 * Mock test for migration programs in the tenant module.
 * Tests SQL file content, execution against real PostgreSQL, idempotency, and status tracking.
 * Uses real config DB and empty DB (no tenant DBs required).
 */
package app.xinqianmao.com.tenant;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
    classes = app.xinqianmao.com.tenant.web.TenantApplication.class,
    properties = {
        "spring.profiles.active=dev",
        "mypet.db.host=127.0.0.1",
        "mypet.db.port=1800",
        "mypet.db.user=postgres",
        "mypet.db.password=mypg123abc"
    }
)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MigrationMockTest {

    @Autowired
    @Qualifier("tenantConfigDataSource")
    private DataSource configDs;

    @Autowired
    @Qualifier("tenantTemplateDataSource")
    private DataSource templateDs;

    @Autowired
    private app.xinqianmao.com.tenant.service.MigrationRunnerService migrationRunner;

    // ============================================================
    // 1. SQL content verification
    // ============================================================

    @Test
    @Order(1)
    @DisplayName("config_002_tenant_admin.sql: exists, idempotent, creates t_admin with super user")
    void sqlConfig002() throws Exception {
        String sql = loadSql("config_002_tenant_admin.sql");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS"), "Must be idempotent");
        assertTrue(sql.contains("t_admin"), "Must create t_admin table");
        assertTrue(sql.contains("gen_random_uuid()"), "Must use UUID primary key");
        assertTrue(sql.contains("super"), "Must have default super admin");
        assertTrue(sql.contains("ON CONFLICT"), "Insert must handle conflicts");
    }

    @Test
    @Order(2)
    @DisplayName("003_admin_security_tables.sql: exists, idempotent, creates both tables")
    void sql003Security() throws Exception {
        String sql = loadSql("003_admin_security_tables.sql");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS c_admin_login_error_log"),
                "Must create error log table idempotently");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS c_admin_login_lock"),
                "Must create lock table idempotently");
        assertTrue(sql.contains("CHAR(36)"), "Must use CHAR(36) PK");
        // database.md says no tenant_code — verify NOT present
        assertFalse(sql.contains("tenant_code"), "Must NOT have tenant_code per database.md");
    }

    // ============================================================
    // 2. splitSql tests (from old admin MigrationRunnerService)
    // ============================================================

    @Test
    @Order(3)
    @DisplayName("splitSql: basic semicolon splitting")
    void splitSqlBasic() {
        String sql = "CREATE TABLE a (id INT);\nCREATE TABLE b (id INT);";
        List<String> parts = splitSql(sql);
        assertEquals(2, parts.size());
    }

    @Test
    @Order(4)
    @DisplayName("splitSql: DO $$ blocks treated as single statement")
    void splitSqlDollarBlock() {
        String sql = "CREATE TABLE t (id INT);\nDO $$\nBEGIN\n  RAISE NOTICE 'x';\nEND $$;\nSELECT 1;";
        List<String> parts = splitSql(sql);
        assertEquals(3, parts.size());
        assertTrue(parts.get(1).contains("DO $$"));
        assertTrue(parts.get(1).contains("END $$;"));
    }

    @Test
    @Order(5)
    @DisplayName("splitSql: strip comments")
    void splitSqlComments() {
        String sql = "-- This is a comment\nCREATE TABLE a (id INT);\n-- Another comment\nCREATE TABLE b (id INT);";
        String cleaned = sql.replaceAll("(?m)^\\s*--.*$", "");
        List<String> parts = splitSql(cleaned);
        assertEquals(2, parts.size());
    }

    // ============================================================
    // 3. Execution tests — run against real config DB
    // ============================================================

    @Test
    @Order(10)
    @DisplayName("Execute config_002_tenant_admin: creates t_admin table + super user")
    void execConfig002() {
        Map<String, Object> result = migrationRunner.runMigration("config_002_tenant_admin.sql");
        System.out.println("config_002 result: " + result);
        assertEquals("success", result.get("status"), "Migration should succeed");

        // Verify table exists
        try (Connection c = configDs.getConnection();
             Statement stmt = c.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT column_name FROM information_schema.columns WHERE table_name='t_admin' ORDER BY ordinal_position");
            List<String> cols = new ArrayList<>();
            while (rs.next()) cols.add(rs.getString(1));
            assertTrue(cols.contains("id"), "Must have id column");
            assertTrue(cols.contains("account"), "Must have account column");
            assertTrue(cols.contains("password"), "Must have password column");
            assertTrue(cols.contains("last_login_time"), "Must have last_login_time column");
        } catch (SQLException e) {
            fail("Table verification failed: " + e.getMessage());
        }

        // Verify super admin exists
        try (Connection c = configDs.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT account, password FROM t_admin WHERE account = 'super'")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next(), "Super admin must exist");
            assertEquals("super", rs.getString("account"));
            assertTrue(rs.getString("password").contains(":"), "Password must be hashed");
        } catch (SQLException e) {
            fail("Super admin check failed: " + e.getMessage());
        }
    }

    @Test
    @Order(11)
    @DisplayName("Execute config_002 AGAIN: idempotency — no errors on re-run")
    void execConfig002Idempotent() {
        Map<String, Object> result = migrationRunner.runMigration("config_002_tenant_admin.sql");
        System.out.println("config_002 idempotent result: " + result);
        assertEquals("success", result.get("status"), "Re-run must succeed (idempotent)");
    }

    @Test
    @Order(12)
    @DisplayName("Execute 003_admin_security_tables: creates tables in empty DB")
    void exec003Security() {
        Map<String, Object> result = migrationRunner.runMigration("003_admin_security_tables.sql");
        System.out.println("003 result: " + result);
        assertEquals("success", result.get("status"), "Migration should succeed");

        // Verify tables exist in template (empty) DB
        for (String table : new String[]{"c_admin_login_error_log", "c_admin_login_lock"}) {
            try (Connection c = templateDs.getConnection();
                 Statement stmt = c.createStatement()) {
                ResultSet rs = stmt.executeQuery(
                    "SELECT column_name FROM information_schema.columns WHERE table_name='" + table +
                    "' AND table_schema='public' ORDER BY ordinal_position");
                List<String> cols = new ArrayList<>();
                while (rs.next()) cols.add(rs.getString(1));
                assertTrue(cols.contains("id"), table + " must have id");
                assertTrue(cols.contains("account"), table + " must have account");
                assertTrue(cols.contains("create_time"), table + " must have create_time");
                // Verify NO tenant_code
                assertFalse(cols.contains("tenant_code"), table + " must NOT have tenant_code per database.md");
            } catch (SQLException e) {
                fail(table + " verification failed: " + e.getMessage());
            }
        }
    }

    @Test
    @Order(13)
    @DisplayName("Execute 003 AGAIN: idempotency — no errors on re-run")
    void exec003Idempotent() {
        Map<String, Object> result = migrationRunner.runMigration("003_admin_security_tables.sql");
        System.out.println("003 idempotent result: " + result);
        assertEquals("success", result.get("status"), "Re-run must succeed (idempotent)");
    }

    // ============================================================
    // 4. Migration status tracking
    // ============================================================

    @Test
    @Order(20)
    @DisplayName("c_migration_log: status tracked for executed migrations")
    void migrationStatusTracking() {
        List<Map<String, Object>> list = migrationRunner.listMigrations();
        assertNotNull(list);
        assertTrue(list.size() >= 2, "Must have at least 2 migrations listed");

        for (Map<String, Object> m : list) {
            String name = (String) m.get("name");
            assertNotNull(name);
            String status = (String) m.get("status");
            assertNotNull(status);

            // Both of our migrations should be marked success after running
            if ("config_002_tenant_admin.sql".equals(name) || "003_admin_security_tables.sql".equals(name)) {
                assertEquals("success", status, name + " must be 'success'");
            }
        }
    }

    @Test
    @Order(21)
    @DisplayName("listMigrations: returns correct target field")
    void listMigrationsTarget() {
        List<Map<String, Object>> list = migrationRunner.listMigrations();
        for (Map<String, Object> m : list) {
            String name = (String) m.get("name");
            String target = (String) m.get("target");
            if (name.startsWith("config_")) {
                assertEquals("config", target, name + " target must be 'config'");
            } else {
                assertEquals("tenant", target, name + " target must be 'tenant'");
            }
        }
    }

    // ============================================================
    // Helpers
    // ============================================================

    private String loadSql(String filename) throws IOException {
        Resource[] all = new PathMatchingResourcePatternResolver()
                .getResources("classpath:sql/migrations/*.sql");
        for (Resource r : all) {
            if (filename.equals(r.getFilename())) {
                return r.getContentAsString(StandardCharsets.UTF_8);
            }
        }
        throw new AssertionError("SQL file not found: " + filename);
    }

    /** Split SQL by ; but preserve DO $$ ... END $$; blocks as single statements. */
    public static List<String> splitSql(String sql) {
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
}
