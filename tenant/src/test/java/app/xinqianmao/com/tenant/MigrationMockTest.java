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

    private static final String CONFIG_FILE = "config_005_consolidated_multi_tenant_and_later.sql";
    private static final String TENANT_FILE = "011_consolidated_tenant_multi_tenant_and_later.sql";

    // ============================================================
    // 1. SQL content verification
    // ============================================================

    @Test
    @Order(1)
    @DisplayName("config_005_consolidated: exists, idempotent, creates t_admin + is_business_open")
    void sqlConfigConsolidated() throws Exception {
        String sql = loadSql(CONFIG_FILE);
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS"), "Must be idempotent");
        assertTrue(sql.contains("t_admin"), "Must create t_admin table");
        assertTrue(sql.contains("gen_random_uuid()"), "Must use UUID primary key");
        assertTrue(sql.contains("super"), "Must have default super admin");
        assertTrue(sql.contains("ON CONFLICT"), "Insert must handle conflicts");
        assertTrue(sql.contains("is_bussiness_open"), "Must add is_bussiness_open column");
        assertTrue(sql.contains("c_tenant"), "Must reference c_tenant");
    }

    @Test
    @Order(2)
    @DisplayName("011_consolidated: exists, idempotent, creates security tables + search + shop")
    void sqlTenantConsolidated() throws Exception {
        String sql = loadSql(TENANT_FILE);
        // Security tables
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS c_admin_login_error_log"),
                "Must create error log table idempotently");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS c_admin_login_lock"),
                "Must create lock table idempotently");
        assertTrue(sql.contains("CHAR(36)"), "Must use CHAR(36) PK");
        assertFalse(sql.contains("tenant_code"), "Must NOT have tenant_code per database.md");
        // Search optimization
        assertTrue(sql.contains("CREATE EXTENSION IF NOT EXISTS pg_jieba"), "Must enable pg_jieba");
        assertTrue(sql.contains("CREATE EXTENSION IF NOT EXISTS pg_trgm"), "Must enable pg_trgm");
        assertTrue(sql.contains("search_text"), "Must add search_text column");
        assertTrue(sql.contains("jiebacfg"), "Must use jiebacfg config");
        assertTrue(sql.contains("gin_trgm_ops"), "Must have trigram index");
        assertTrue(sql.contains("idx_product_name"), "Must have name index");
        assertTrue(sql.contains("UPDATE t_product"), "Must backfill search_text");
        // Shop detail + contact
        assertTrue(sql.contains("ALTER TABLE t_shop"), "Must alter t_shop");
        assertTrue(sql.contains("detail TEXT"), "Must add detail column");
        assertTrue(sql.contains("contact"), "Must add contact column");
    }

    // ============================================================
    // 2. splitSql tests
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
    @DisplayName("Execute config_005_consolidated: creates t_admin + adds is_business_open")
    void execConfigConsolidated() {
        Map<String, Object> result = migrationRunner.runMigration(CONFIG_FILE);
        System.out.println("config_005 result: " + result);
        assertEquals("success", result.get("status"), "Migration should succeed");

        // Verify t_admin table exists
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
            fail("t_admin verification failed: " + e.getMessage());
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

        // Verify is_bussiness_open column
        try (Connection c = configDs.getConnection();
             Statement stmt = c.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT column_name FROM information_schema.columns WHERE table_name='c_tenant' AND column_name='is_bussiness_open'");
            assertTrue(rs.next(), "is_bussiness_open column must exist on c_tenant");
        } catch (SQLException e) {
            fail("is_bussiness_open check failed: " + e.getMessage());
        }
    }

    @Test
    @Order(11)
    @DisplayName("Execute config_005 AGAIN: idempotency — no errors on re-run")
    void execConfigConsolidatedIdempotent() {
        Map<String, Object> result = migrationRunner.runMigration(CONFIG_FILE);
        System.out.println("config_005 idempotent result: " + result);
        assertEquals("success", result.get("status"), "Re-run must succeed (idempotent)");
    }

    @Test
    @Order(12)
    @DisplayName("Execute 011_consolidated: creates security tables + search + shop in empty DB")
    void execTenantConsolidated() {
        Map<String, Object> result = migrationRunner.runMigration(TENANT_FILE);
        System.out.println("011 result: " + result);
        assertEquals("success", result.get("status"), "Migration should succeed");

        // Verify security tables in template (empty) DB
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
                assertFalse(cols.contains("tenant_code"), table + " must NOT have tenant_code per database.md");
            } catch (SQLException e) {
                fail(table + " verification failed: " + e.getMessage());
            }
        }

        // Verify search_text column + indexes on t_product in empty DB
        try (Connection c = templateDs.getConnection();
             Statement stmt = c.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT column_name FROM information_schema.columns WHERE table_name='t_product' AND column_name='search_text'");
            assertTrue(rs.next(), "search_text column must exist on t_product");
        } catch (SQLException e) {
            fail("search_text check failed: " + e.getMessage());
        }

        // Verify detail column on t_shop
        try (Connection c = templateDs.getConnection();
             Statement stmt = c.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT column_name FROM information_schema.columns WHERE table_name='t_shop' AND column_name='detail'");
            assertTrue(rs.next(), "detail column must exist on t_shop");
        } catch (SQLException e) {
            fail("detail check failed: " + e.getMessage());
        }

        // Verify contact column on t_shop
        try (Connection c = templateDs.getConnection();
             Statement stmt = c.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT column_name FROM information_schema.columns WHERE table_name='t_shop' AND column_name='contact'");
            assertTrue(rs.next(), "contact column must exist on t_shop");
        } catch (SQLException e) {
            fail("contact check failed: " + e.getMessage());
        }
    }

    @Test
    @Order(13)
    @DisplayName("Execute 011 AGAIN: idempotency — no errors on re-run")
    void execTenantConsolidatedIdempotent() {
        Map<String, Object> result = migrationRunner.runMigration(TENANT_FILE);
        System.out.println("011 idempotent result: " + result);
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

            if (CONFIG_FILE.equals(name) || TENANT_FILE.equals(name)) {
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
