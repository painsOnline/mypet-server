/**
 * File: MigrationRefactorTest.java
 * Author: system
 * Date: 2026-05-18
 *
 * Mock tests for 002_refactor_specs_props_sku migration (README line 172-173).
 * All tests simulate pre-migration state with mock tables on config DB.
 * No dependency on real tenant data or prior migration state.
 */
package app.xinqianmao.com.admin;

import app.xinqianmao.com.common.migrate.BatchMigrateSpecs;
import app.xinqianmao.com.common.migrate.ConsoleLogger;
import app.xinqianmao.com.common.migrate.MigrationLogger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = app.xinqianmao.com.admin.web.AdminApplication.class)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MigrationRefactorTest {

    @Autowired
    @Qualifier("configDataSource")
    private DataSource ds;

    // ============================================================
    // 1. SQL files exist and contain required constructs
    // ============================================================

    @Test
    @DisplayName("002a_setup.sql: exists, idempotent, has backup + value table + specs_new")
    void sql002aSetup() throws Exception {
        String sql = loadMigrationSql("002a_setup.sql");
        assertTrue(sql.contains("bak_"), "Must backup tables");
        assertTrue(sql.contains("IF NOT EXISTS"), "Must be idempotent");
        assertTrue(sql.contains("t_product_specs_value"), "Must create value table");
        assertTrue(sql.contains("input_options"), "Must migrate input_options");
        assertTrue(sql.contains("value_id"), "Must add value_id column");
        assertTrue(sql.contains("specs_new"), "Must add specs_new columns");
        assertTrue(sql.contains("product_id"), "Must add product_id to cart");
    }

    @Test
    @DisplayName("002b_finalize.sql: exists, has validation+swap+gin+cleanup")
    void sql002bFinalize() throws Exception {
        String sql = loadMigrationSql("002b_finalize.sql");
        assertTrue(sql.contains("RAISE EXCEPTION"), "Must validate before DROP");
        assertTrue(sql.contains("DROP COLUMN specs"), "Must swap columns");
        assertTrue(sql.contains("RENAME"), "Must rename specs_new to specs");
        assertTrue(sql.contains("USING GIN"), "Must create GIN indexes");
        assertTrue(sql.contains("input_options"), "Must drop deprecated column");
        assertTrue(sql.contains("DROP COLUMN IF EXISTS price"), "Must drop cart price columns");
    }

    // ============================================================
    // 2. SQL splitting (DO $$ blocks)
    // ============================================================

    @Test
    @DisplayName("splitSql: DO $$ ... END $$; treated as single statement")
    void splitSqlDollarBlock() {
        String sql = "CREATE TABLE t (id INT);\nDO $$\nBEGIN\n  RAISE NOTICE 'x';\nEND $$;\nSELECT 1;";
        List<String> parts = app.xinqianmao.com.admin.service.MigrationRunnerService.splitSql(sql);
        assertEquals(3, parts.size());
        assertTrue(parts.get(1).contains("DO $$"));
        assertTrue(parts.get(1).contains("END $$;"));
    }

    @Test
    @DisplayName("splitSql: multiple DO blocks")
    void splitSqlMultiDollar() {
        String sql = "DO $$\nBEGIN\nEND $$;\nDO $$\nBEGIN\nEND $$;";
        List<String> parts = app.xinqianmao.com.admin.service.MigrationRunnerService.splitSql(sql);
        assertEquals(2, parts.size());
    }

    // ============================================================
    // 3. Mock STEP 1+2: create t_product_specs_value + migrate input_options
    // ============================================================

    @Test
    @DisplayName("STEP2: input_options migrated to t_product_specs_value")
    void step2MigrateInputOptions() throws Exception {
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            // Pre-migration state: t_product_specs has input_options VARCHAR(255)[]
            s.execute("DROP TABLE IF EXISTS mock_specs CASCADE");
            s.execute("DROP TABLE IF EXISTS mock_specs_value CASCADE");
            s.execute("CREATE TABLE mock_specs (id CHAR(36) PRIMARY KEY, name VARCHAR(100), "
                + "input_type INT, input_options VARCHAR(255)[])");
            // Insert a spec with input_options (simulating production data)
            s.execute("INSERT INTO mock_specs (id, name, input_type, input_options) VALUES "
                + "('s1', '默认规格', 1, ARRAY['2kg*1袋','1.5kg*1袋','1kg*1袋'])");
            s.execute("INSERT INTO mock_specs (id, name, input_type, input_options) VALUES "
                + "('s2', '口味', 2, ARRAY['牛肉味','鸡肉味'])");

            // Create value table
            s.execute("CREATE TABLE mock_specs_value ("
                + "id CHAR(36) PRIMARY KEY, specs_id CHAR(36) NOT NULL, "
                + "value_name VARCHAR(255) NOT NULL, sort INT DEFAULT 0, "
                + "UNIQUE(specs_id, value_name))");

            // Simulate STEP 2: migrate input_options array → value table rows
            // (This is what the DO block in 002a_setup.sql does)
            ResultSet rs = s.executeQuery("SELECT id, input_options FROM mock_specs WHERE input_options IS NOT NULL");
            List<Object[]> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(new Object[]{rs.getString("id"), rs.getArray("input_options").getArray()});
            }
            rs.close();
            try (Statement s2 = c.createStatement()) {
                for (Object[] row : rows) {
                    String specId = (String) row[0];
                    String[] options = (String[]) row[1];
                    for (int i = 0; i < options.length; i++) {
                        s2.execute("INSERT INTO mock_specs_value (id, specs_id, value_name, sort) VALUES "
                            + "('" + UUID.randomUUID() + "', '" + specId + "', '" + options[i] + "', " + i + ") "
                            + "ON CONFLICT (specs_id, value_name) DO NOTHING");
                    }
                }
            }

            // Verify: should have 5 values (3 + 2)
            ResultSet rs2 = s.executeQuery("SELECT COUNT(*) FROM mock_specs_value");
            assertTrue(rs2.next());
            assertEquals(5, rs2.getInt(1), "Should have 5 values from input_options");

            // Verify: idempotent re-run does not duplicate
            s.execute("INSERT INTO mock_specs_value (id, specs_id, value_name, sort) VALUES "
                + "('" + UUID.randomUUID() + "', 's1', '2kg*1袋', 0) "
                + "ON CONFLICT (specs_id, value_name) DO NOTHING");
            ResultSet rs3 = s.executeQuery("SELECT COUNT(*) FROM mock_specs_value");
            assertTrue(rs3.next());
            assertEquals(5, rs3.getInt(1), "Idempotent: no duplicates");

            s.execute("DROP TABLE IF EXISTS mock_specs CASCADE");
            s.execute("DROP TABLE IF EXISTS mock_specs_value CASCADE");
        }
    }

    // ============================================================
    // 4. Mock STEP 6: convertSpecs with proper value mappings
    // ============================================================

    @Test
    @DisplayName("STEP6: convertSpecs succeeds when t_product_specs_value has matching entries")
    void step6ConvertSpecsWithValidValues() {
        // Simulate state AFTER STEP 2: specs and values loaded from migrated data
        Map<String, String> specNameToId = Map.of("默认规格", "s1", "口味", "s2");
        Map<String, Integer> specIdToInputType = Map.of("s1", 1, "s2", 2);
        Map<String, String> valueKeyToId = Map.of(
            "s1|2kg*1袋", "v1", "s1|1.5kg*1袋", "v2", "s1|1kg*1袋", "v3",
            "s2|牛肉味", "v4", "s2|鸡肉味", "v5");

        // Old-format SKU specs from the real data pattern
        String oldJson = "[{\"name\":\"默认规格\",\"valueName\":\"2kg*1袋\"},"
            + "{\"name\":\"口味\",\"valueName\":\"牛肉味\"}]";

        String result = invokeConvertSpecs(oldJson, specNameToId, valueKeyToId, specIdToInputType, true);
        assertNotNull(result, "Should produce valid new-format JSON");

        // For t_product_sku: value_name only for input_type=1
        assertTrue(result.contains("\"spec_id\":\"s1\""));
        assertTrue(result.contains("\"value_id\":\"v1\""));
        assertTrue(result.contains("\"value_name\":\"2kg*1袋\""), "input_type=1 should keep value_name");
        assertTrue(result.contains("\"spec_id\":\"s2\""));
        assertTrue(result.contains("\"value_id\":\"v4\""));
        assertTrue(result.contains("\"value_name\":\"\""), "input_type=2 should have empty value_name");
    }

    @Test
    @DisplayName("STEP6: t_order_product_skus format — all 4 fields non-null")
    void step6ConvertSpecsForOrderSkus() {
        Map<String, String> specNameToId = Map.of("默认规格", "s1");
        Map<String, Integer> specIdToInputType = Map.of("s1", 1);
        Map<String, String> valueKeyToId = Map.of("s1|2kg*1袋", "v1");

        String oldJson = "[{\"name\":\"默认规格\",\"valueName\":\"2kg*1袋\"}]";
        String result = invokeConvertSpecs(oldJson, specNameToId, valueKeyToId, specIdToInputType, false);
        assertNotNull(result);
        assertTrue(result.contains("\"value_name\":\"2kg*1袋\""), "order_skus: value_name always populated");
        assertFalse(result.contains("\"value_name\":\"\""), "order_skus: value_name never empty");
    }

    // ============================================================
    // 5. REQUIREMENT: 三张表 specs 字段格式校验（README 迭代要求）
    // ============================================================

    private static final Map<String, String> LOOKUP_NAME_TO_ID = Map.of("默认规格", "s1", "口味", "s2");
    private static final Map<String, Integer> LOOKUP_TYPE = Map.of("s1", 1, "s2", 2);
    private static final Map<String, String> LOOKUP_VAL = Map.of(
        "s1|2kg*1袋", "v1", "s1|1.5kg*1袋", "v2",
        "s2|牛肉味", "v3", "s2|鸡肉味", "v4");

    @Test
    @DisplayName("REQ1: t_product_sku — spec_id/value_id never empty, value_name only for input_type=1")
    void req1ProductSkuSpecsFormat() {
        String oldJson = "[{\"name\":\"默认规格\",\"valueName\":\"2kg*1袋\"}"
            + ",{\"name\":\"口味\",\"valueName\":\"牛肉味\"}]";
        String result = invokeConvertSpecs(oldJson, LOOKUP_NAME_TO_ID, LOOKUP_VAL, LOOKUP_TYPE, true);
        assertNotNull(result);

        List<Map<String, String>> parsed = parseNewJson(result);
        assertEquals(2, parsed.size());

        // Element 1: 默认规格 (input_type=1 → value_name populated)
        Map<String, String> e1 = parsed.get(0);
        assertFalse(e1.get("spec_id").isEmpty(), "t_product_sku: spec_id must not be empty");
        assertFalse(e1.get("value_id").isEmpty(), "t_product_sku: value_id must not be empty");
        assertFalse(e1.get("value_name").isEmpty(), "t_product_sku: value_name must be populated for input_type=1");
        assertEquals("2kg*1袋", e1.get("value_name"));

        // Element 2: 口味 (input_type=2 → value_name empty)
        Map<String, String> e2 = parsed.get(1);
        assertFalse(e2.get("spec_id").isEmpty(), "t_product_sku: spec_id must not be empty");
        assertFalse(e2.get("value_id").isEmpty(), "t_product_sku: value_id must not be empty");
        assertEquals("", e2.get("value_name"), "t_product_sku: value_name must be empty for input_type≠1");
    }

    @Test
    @DisplayName("REQ2: t_order_product_skus — spec_id/spec_name/value_id/value_name all never empty")
    void req2OrderProductSkusSpecsFormat() {
        String oldJson = "[{\"name\":\"默认规格\",\"valueName\":\"2kg*1袋\"}"
            + ",{\"name\":\"口味\",\"valueName\":\"牛肉味\"}]";
        String result = invokeConvertSpecs(oldJson, LOOKUP_NAME_TO_ID, LOOKUP_VAL, LOOKUP_TYPE, false);
        assertNotNull(result);

        List<Map<String, String>> parsed = parseNewJson(result);
        assertEquals(2, parsed.size());

        for (int i = 0; i < parsed.size(); i++) {
            Map<String, String> e = parsed.get(i);
            assertFalse(e.get("spec_id").isEmpty(),
                "t_order_product_skus[" + i + "]: spec_id must not be empty");
            assertFalse(e.get("spec_name").isEmpty(),
                "t_order_product_skus[" + i + "]: spec_name must not be empty");
            assertFalse(e.get("value_id").isEmpty(),
                "t_order_product_skus[" + i + "]: value_id must not be empty");
            assertFalse(e.get("value_name").isEmpty(),
                "t_order_product_skus[" + i + "]: value_name must not be empty");
        }
    }

    @Test
    @DisplayName("REQ3: t_cart — spec_id/spec_name/value_id/value_name all never empty")
    void req3CartSpecsFormat() {
        // Cart uses same conversion as order_skus (isProductSku=false)
        String oldJson = "[{\"name\":\"默认规格\",\"valueName\":\"1.5kg*1袋\"}]";
        String result = invokeConvertSpecs(oldJson, LOOKUP_NAME_TO_ID, LOOKUP_VAL, LOOKUP_TYPE, false);
        assertNotNull(result);

        List<Map<String, String>> parsed = parseNewJson(result);
        assertEquals(1, parsed.size());

        Map<String, String> e = parsed.get(0);
        assertFalse(e.get("spec_id").isEmpty(), "t_cart: spec_id must not be empty");
        assertFalse(e.get("spec_name").isEmpty(), "t_cart: spec_name must not be empty");
        assertFalse(e.get("value_id").isEmpty(), "t_cart: value_id must not be empty");
        assertFalse(e.get("value_name").isEmpty(), "t_cart: value_name must not be empty");
    }

    // ============================================================
    // 6. Edge cases (bug fixes verified by previous runs)
    // ============================================================

    @Test
    @DisplayName("Edge: all-unmatched returns null → caller sets specs_new='[]' (no infinite loop)")
    void edgeAllUnmatchedReturnsNull() {
        Map<String, String> specNameToId = Map.of("规格A", "sa1"); // different from JSON
        Map<String, Integer> specIdToInputType = Map.of();
        Map<String, String> valueKeyToId = Map.of();

        String oldJson = "[{\"name\":\"不存在的规格\",\"valueName\":\"X\"}]";
        String result = invokeConvertSpecs(oldJson, specNameToId, valueKeyToId, specIdToInputType, false);

        assertNull(result, "All-unmatched → null → caller MUST set specs_new='[]' to prevent re-selection");
    }

    @Test
    @DisplayName("Edge: empty specs array returns null")
    void edgeEmptyArrayReturnsNull() {
        Map<String, String> specNameToId = Map.of();
        Map<String, Integer> specIdToInputType = Map.of();
        Map<String, String> valueKeyToId = Map.of();
        assertNull(invokeConvertSpecs("[]", specNameToId, valueKeyToId, specIdToInputType, false));
    }

    @Test
    @DisplayName("Edge: already-new-format passes through without modification")
    void edgeAlreadyNewFormat() {
        Map<String, String> specNameToId = Map.of("规格", "s1");
        Map<String, Integer> specIdToInputType = Map.of("s1", 1);
        Map<String, String> valueKeyToId = Map.of("s1|2.5Kg", "v1");

        String alreadyNew = "[{\"spec_id\":\"s1\",\"spec_name\":\"规格\",\"value_name\":\"2.5Kg\",\"value_id\":\"v1\"}]";
        String result = invokeConvertSpecs(alreadyNew, specNameToId, valueKeyToId, specIdToInputType, true);
        assertNotNull(result);
        assertTrue(result.contains("\"spec_id\":\"s1\""));
    }

    @Test
    @DisplayName("Edge: partial match — some elements match, some don't")
    void edgePartialMatch() {
        Map<String, String> specNameToId = Map.of("规格A", "sa1"); // only 规格A exists
        Map<String, Integer> specIdToInputType = Map.of("sa1", 1);
        Map<String, String> valueKeyToId = Map.of("sa1|值A", "va1");

        // First element matches, second doesn't
        String oldJson = "[{\"name\":\"规格A\",\"valueName\":\"值A\"},"
            + "{\"name\":\"不存在的\",\"valueName\":\"X\"}]";
        String result = invokeConvertSpecs(oldJson, specNameToId, valueKeyToId, specIdToInputType, true);

        assertNotNull(result, "Partial match should still produce result");
        assertTrue(result.contains("sa1"), "Matching element should be in result");
        assertFalse(result.contains("不存在的"), "Unmatched element should be skipped");
        // Single element in result
        assertEquals(1, result.split("\"spec_id\"").length - 1, "Only 1 element should be in result");
    }

    // ============================================================
    // 6. Mock STEP 7: validation+swap protection
    // ============================================================

    @Test
    @DisplayName("STEP7: validation DO block prevents DROP when counts mismatch")
    void step7ValidationPreventsSwap() throws Exception {
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            s.execute("DROP TABLE IF EXISTS mock_sku CASCADE");
            s.execute("CREATE TABLE mock_sku (id CHAR(36) PRIMARY KEY, specs JSON, specs_new JSONB)");
            s.execute("INSERT INTO mock_sku (id, specs, specs_new) VALUES "
                + "('id1', '[{\"name\":\"X\"}]', NULL), "
                + "('id2', '[{\"name\":\"Y\"}]', NULL)");

            // specs has 2 rows, specs_new has 0 → validation should fail
            boolean swapPrevented = false;
            try {
                s.execute("DO $$ DECLARE old INT; new INT; BEGIN "
                    + "SELECT COUNT(*) INTO old FROM mock_sku WHERE specs IS NOT NULL; "
                    + "SELECT COUNT(*) INTO new FROM mock_sku WHERE specs_new IS NOT NULL; "
                    + "IF old != new THEN RAISE EXCEPTION 'mock_sku: specs_new(%) != specs(%), abort', new, old; END IF; "
                    + "ALTER TABLE mock_sku DROP COLUMN specs; "
                    + "ALTER TABLE mock_sku RENAME COLUMN specs_new TO specs; "
                    + "END $$;");
            } catch (SQLException e) {
                swapPrevented = e.getMessage().contains("abort");
            }
            assertTrue(swapPrevented, "Validation should prevent DROP+RENAME on mismatch");

            // Verify: specs column still exists (DROP was prevented)
            ResultSet rs = s.executeQuery("SELECT column_name FROM information_schema.columns "
                + "WHERE table_name='mock_sku' AND column_name='specs'");
            assertTrue(rs.next(), "specs column should still exist");

            s.execute("DROP TABLE IF EXISTS mock_sku CASCADE");
        }
    }

    @Test
    @DisplayName("STEP7: validation passes → swap executes")
    void step7ValidationPassesSwapExecutes() throws Exception {
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            s.execute("DROP TABLE IF EXISTS mock_sku2 CASCADE");
            s.execute("CREATE TABLE mock_sku2 (id CHAR(36) PRIMARY KEY, specs JSON, specs_new JSONB)");
            s.execute("INSERT INTO mock_sku2 (id, specs, specs_new) VALUES "
                + "('id1', '[{\"name\":\"X\"}]', '[{\"a\":\"b\"}]')");

            // specs has 1, specs_new has 1 → validation passes → swap executes
            s.execute("DO $$ DECLARE old INT; new INT; BEGIN "
                + "SELECT COUNT(*) INTO old FROM mock_sku2 WHERE specs IS NOT NULL; "
                + "SELECT COUNT(*) INTO new FROM mock_sku2 WHERE specs_new IS NOT NULL; "
                + "IF old != new THEN RAISE EXCEPTION 'abort'; END IF; "
                + "ALTER TABLE mock_sku2 DROP COLUMN specs; "
                + "ALTER TABLE mock_sku2 RENAME COLUMN specs_new TO specs; "
                + "END $$;");

            // Verify: specs_new no longer exists (renamed), specs is the new JSONB column
            ResultSet rs = s.executeQuery("SELECT column_name, data_type FROM information_schema.columns "
                + "WHERE table_name='mock_sku2' ORDER BY column_name");
            Set<String> cols = new HashSet<>();
            while (rs.next()) cols.add(rs.getString("column_name"));
            assertTrue(cols.contains("specs"), "specs should exist (renamed from specs_new)");
            assertFalse(cols.contains("specs_new"), "specs_new should not exist");

            s.execute("DROP TABLE IF EXISTS mock_sku2 CASCADE");
        }
    }

    // ============================================================
    // Helpers
    // ============================================================

    private String loadMigrationSql(String name) throws Exception {
        var r = new org.springframework.core.io.support.PathMatchingResourcePatternResolver();
        var resources = r.getResources("classpath:sql/migrations/" + name);
        assertTrue(resources.length > 0, name + " should exist in classpath");
        return resources[0].getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    }

    /** Parse new-format JSON array string into list of key-value maps for assertion. */
    static List<Map<String, String>> parseNewJson(String json) {
        List<Map<String, String>> result = new ArrayList<>();
        if (json == null || json.length() < 2) return result;
        // Simple parser for: [{"k":"v","k2":"v2"},{"k":"v"}]
        int depth = 0, start = -1;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') {
                if (depth == 0) start = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    Map<String, String> map = new LinkedHashMap<>();
                    String[] pairs = json.substring(start + 1, i).split(",");
                    for (String pair : pairs) {
                        int colon = pair.indexOf(':');
                        if (colon < 0) continue;
                        String key = pair.substring(0, colon).trim().replace("\"", "");
                        String value = pair.substring(colon + 1).trim().replace("\"", "");
                        map.put(key, value);
                    }
                    result.add(map);
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    static String invokeConvertSpecs(String specsJson,
                                      Map<String, String> specNameToId,
                                      Map<String, String> valueKeyToId,
                                      Map<String, Integer> specIdToInputType,
                                      boolean isProductSku) {
        try {
            var method = BatchMigrateSpecs.class.getDeclaredMethod("convertSpecs",
                String.class, Map.class, Map.class, Map.class, boolean.class,
                MigrationLogger.class, int.class);
            method.setAccessible(true);
            return (String) method.invoke(null, specsJson, specNameToId, valueKeyToId,
                specIdToInputType, isProductSku, new ConsoleLogger(), 0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
