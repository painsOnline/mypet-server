/**
 * File: BatchMigrateSpecs.java
 * Author: system
 * Date: 2026-05-18
 *
 * Hybrid migration: Java batch processing for heavy specs data migration.
 * Preloads lookup maps scoped by product_type (via t_product_type_spec_rel),
 * reads source rows in batches of 500, converts old JSON format to new format
 * in Java memory, UPDATEs in small transactions. Resumable.
 */
package app.xinqianmao.com.common.migrate;

import java.sql.*;
import java.util.*;

public final class BatchMigrateSpecs {
    private static final int BATCH = 500;

    /**
     * Migrate specs JSON data for one table.
     */
    public static void migrateTable(Connection c, String table, String keyColumns,
                                     boolean isProductSku, MigrationLogger log) throws Exception {
        log.info("  " + table + ": loading lookup maps...");

        // ---- Global maps ----
        // specId -> input_type
        Map<String, Integer> specIdToInputType = new LinkedHashMap<>();
        // "specs_id|value_name" -> value_id
        Map<String, String> valueKeyToId = new LinkedHashMap<>();
        // product_type -> (spec_name -> spec_id)
        Map<String, Map<String, String>> productTypeSpecNames = new LinkedHashMap<>();
        // product_id -> product_type
        Map<String, String> productIdToType = new LinkedHashMap<>();

        try (Statement s = c.createStatement()) {
            // Load spec input types
            ResultSet rs = s.executeQuery("SELECT id, input_type FROM t_product_specs");
            while (rs.next()) specIdToInputType.put(rs.getString("id"), rs.getInt("input_type"));
            rs.close();

            // Load values
            rs = s.executeQuery("SELECT id, specs_id, value_name FROM t_product_specs_value");
            while (rs.next())
                valueKeyToId.put(rs.getString("specs_id") + "|" + rs.getString("value_name"), rs.getString("id"));
            rs.close();

            // Load product_type -> spec_names mapping (via t_product_type_spec_rel)
            rs = s.executeQuery(
                "SELECT rel.product_type, ps.name, ps.id " +
                "FROM t_product_type_spec_rel rel JOIN t_product_specs ps ON rel.specs_id = ps.id " +
                "ORDER BY rel.product_type");
            while (rs.next()) {
                String pt = rs.getString("product_type");
                productTypeSpecNames.computeIfAbsent(pt, k -> new LinkedHashMap<>())
                    .put(rs.getString("name"), rs.getString("id"));
            }
            rs.close();

            // Load product_id -> product_type
            rs = s.executeQuery("SELECT id, product_type FROM t_product");
            while (rs.next()) productIdToType.put(rs.getString("id"), rs.getString("product_type"));
            rs.close();
        }
        log.info("  " + table + ": " + specIdToInputType.size() + " specs, " + valueKeyToId.size()
            + " values, " + productTypeSpecNames.size() + " product types, " + productIdToType.size() + " products");

        // Ensure specs_new column exists
        try (Statement s = c.createStatement()) {
            s.execute("ALTER TABLE " + table + " ADD COLUMN IF NOT EXISTS specs_new JSONB");
        }

        // Count remaining
        int total;
        try (Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(
                 "SELECT COUNT(*) FROM " + table + " WHERE specs IS NOT NULL AND specs_new IS NULL")) {
            rs.next(); total = rs.getInt(1);
        }
        if (total == 0) { log.info("  " + table + ": already migrated, skip"); return; }
        log.info("  " + table + ": " + total + " rows to migrate...");

        String[] keys = keyColumns.split(",");
        // Check if table has product_id column
        boolean hasProductId = true;
        try (Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(
                 "SELECT column_name FROM information_schema.columns " +
                 "WHERE table_name='" + table + "' AND column_name='product_id'")) {
            hasProductId = rs.next();
        }

        int processed = 0, skipped = 0, warnCount = 0;
        long start = System.currentTimeMillis();

        String selectCols = keyColumns + ", specs" + (hasProductId ? ", product_id" : "");
        while (true) {
            List<Object[]> batch = new ArrayList<>();
            try (Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery(
                     "SELECT " + selectCols + " FROM " + table +
                     " WHERE specs IS NOT NULL AND specs_new IS NULL LIMIT " + BATCH)) {
                int colCount = rs.getMetaData().getColumnCount();
                while (rs.next()) {
                    Object[] row = new Object[keys.length + 3]; // keys + oldJson + newJson + productType
                    for (int i = 0; i < keys.length; i++) row[i] = rs.getString(i + 1);
                    String oldJson = rs.getString(keys.length + 1);
                    row[keys.length] = oldJson;

                    // Determine product_type for this row
                    String productType = null;
                    if (hasProductId && colCount >= keys.length + 2) {
                        String productId = rs.getString(keys.length + 2);
                        if (productId != null) productType = productIdToType.get(productId);
                    }

                    // Get scoped lookup map for this product_type
                    Map<String, String> scopedSpecNames = (productType != null)
                        ? productTypeSpecNames.get(productType)
                        : null;
                    // Fallback: if no product_type match, use empty map (will skip all)
                    if (scopedSpecNames == null) scopedSpecNames = Collections.emptyMap();

                    String newJson = convertSpecs(oldJson, scopedSpecNames, valueKeyToId,
                        specIdToInputType, isProductSku, log, warnCount);
                    if (newJson == null && warnCount < 10) warnCount++;
                    row[keys.length + 1] = newJson != null ? newJson : "[]";
                    row[keys.length + 2] = productType;
                    batch.add(row);
                }
            }
            if (batch.isEmpty()) break;

            String updateSql = buildUpdateSql(table, keys);
            try (PreparedStatement ps = c.prepareStatement(updateSql)) {
                for (Object[] row : batch) {
                    String newJson = (String) row[keys.length + 1];
                    ps.setString(1, newJson);
                    for (int i = 0; i < keys.length; i++) ps.setString(i + 2, (String) row[i]);
                    ps.addBatch();
                    if ("[]".equals(newJson)) skipped++;
                }
                ps.executeBatch();
                c.commit();
            }
            processed += batch.size();
            if (processed % 5000 < BATCH) {
                long elapsed = (System.currentTimeMillis() - start) / 1000;
                log.info("  " + table + ": " + processed + "/" + total + " (" +
                    (processed * 100 / total) + "%), " + elapsed + "s");
            }
        }
        long elapsed = (System.currentTimeMillis() - start) / 1000;
        log.info("  " + table + ": DONE. " + processed + " rows, " + skipped + " skipped, " + elapsed + "s");
    }

    static String buildUpdateSql(String table, String[] keys) {
        StringBuilder sb = new StringBuilder("UPDATE " + table + " SET specs_new = ?::jsonb WHERE ");
        for (int i = 0; i < keys.length; i++) {
            if (i > 0) sb.append(" AND ");
            sb.append(keys[i].trim() + " = ?::uuid");
        }
        return sb.toString();
    }

    /** Convert old-format specs JSON to new format, using product_type-scoped spec lookup. */
    static String convertSpecs(String specsJson,
                               Map<String, String> scopedSpecNames,
                               Map<String, String> valueKeyToId,
                               Map<String, Integer> specIdToInputType,
                               boolean isProductSku,
                               MigrationLogger log, int warnCount) {
        try {
            List<Map<String, String>> oldElems = parseJsonArray(specsJson);
            if (oldElems.isEmpty()) return null;

            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Map<String, String> elem : oldElems) {
                String specName = elem.getOrDefault("spec_name", elem.get("name"));
                if (specName == null) specName = "";
                String specValue = elem.getOrDefault("value_name", elem.get("valueName"));
                if (specValue == null) specValue = "";

                String specId = elem.get("spec_id");
                if (specId == null || specId.isEmpty()) specId = scopedSpecNames.get(specName);
                if (specId == null || specId.isEmpty()) {
                    // name mismatch — pick first type-1 spec from this product_type, if any
                    for (Map.Entry<String, String> e : scopedSpecNames.entrySet()) {
                        Integer t = specIdToInputType.get(e.getValue());
                        if (t != null && t == 1) {
                            specId = e.getValue();
                            specName = e.getKey(); // use the DB spec name, not old JSON name
                            break;
                        }
                    }
                }
                if (specId == null || specId.isEmpty()) {
                    if (warnCount < 10) log.warn("spec_name='" + specName + "' not found for this product_type");
                    continue;
                }

                String valueId = elem.get("value_id");
                if (valueId == null || valueId.isEmpty()) valueId = valueKeyToId.get(specId + "|" + specValue);
                if (valueId == null || valueId.isEmpty()) {
                    Integer it = specIdToInputType.get(specId);
                    if (it != null && it == 1) {
                        String prefix = specId + "|";
                        for (Map.Entry<String, String> e : valueKeyToId.entrySet()) {
                            if (e.getKey().startsWith(prefix)) { valueId = e.getValue(); break; }
                        }
                    }
                }
                if (valueId == null || valueId.isEmpty()) {
                    if (warnCount < 10) log.warn("value '" + specName + "|" + specValue + "' not found");
                    continue;
                }

                String valueName;
                if (isProductSku) {
                    Integer inputType = specIdToInputType.get(specId);
                    valueName = (inputType != null && inputType == 1) ? specValue : "";
                } else {
                    valueName = specValue;
                }

                if (!first) sb.append(",");
                first = false;
                sb.append("{\"spec_id\":\"").append(escapeJson(specId))
                  .append("\",\"spec_name\":\"").append(escapeJson(specName))
                  .append("\",\"value_name\":\"").append(escapeJson(valueName))
                  .append("\",\"value_id\":\"").append(escapeJson(valueId))
                  .append("\"}");
            }
            sb.append("]");
            return first ? null : sb.toString();
        } catch (Exception e) {
            log.error("ERROR parsing specs: " + e.getMessage());
            return null;
        }
    }

    // ---- Minimal JSON parser ----

    static List<Map<String, String>> parseJsonArray(String json) {
        List<Map<String, String>> result = new ArrayList<>();
        if (json == null) return result;
        json = json.trim();
        if (!json.startsWith("[")) return result;
        int i = 1;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') { i++; continue; }
            if (c == ']') break;
            if (c == ',') { i++; continue; }
            if (c == '{') {
                int end = findMatchingBrace(json, i);
                result.add(parseJsonObject(json.substring(i + 1, end)));
                i = end + 1;
            } else { i++; }
        }
        return result;
    }

    static int findMatchingBrace(String s, int start) {
        int depth = 0;
        boolean inString = false;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' && (i == 0 || s.charAt(i - 1) != '\\')) inString = !inString;
            if (inString) continue;
            if (c == '{') depth++;
            if (c == '}') { depth--; if (depth == 0) return i; }
        }
        return s.length() - 1;
    }

    static Map<String, String> parseJsonObject(String content) {
        Map<String, String> map = new LinkedHashMap<>();
        boolean inString = false;
        StringBuilder key = null, current = new StringBuilder();
        boolean readingKey = true;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '"' && (i == 0 || content.charAt(i - 1) != '\\')) { inString = !inString; continue; }
            if (inString) { current.append(c); }
            else if (c == ':' && readingKey) { key = new StringBuilder(current.toString().trim()); current = new StringBuilder(); readingKey = false; }
            else if (c == ',' && !readingKey) {
                String v = current.toString().trim();
                if (v.startsWith("\"") && v.endsWith("\"")) v = v.substring(1, v.length() - 1);
                if (key != null) map.put(key.toString(), v);
                key = null; current = new StringBuilder(); readingKey = true;
            } else if (c != ' ' && c != '\n' && c != '\r' && c != '\t') { current.append(c); }
        }
        String v = current.toString().trim();
        if (v.startsWith("\"") && v.endsWith("\"")) v = v.substring(1, v.length() - 1);
        if (key != null) map.put(key.toString(), v);
        return map;
    }

    static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
