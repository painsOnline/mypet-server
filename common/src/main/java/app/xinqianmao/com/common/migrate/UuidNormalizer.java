/**
 * File: UuidNormalizer.java
 * Author: system
 * Date: 2026-05-18
 *
 * Normalize all 32-char UUIDs → 36-char (with dashes).
 * Targets CHAR(36) columns and JSON/JSONB content.
 * Skips VARCHAR columns (image paths).
 * Idempotent: only modifies values that don't contain '-'.
 */
package app.xinqianmao.com.common.migrate;

import java.sql.*;
import java.util.*;

public final class UuidNormalizer {
    private static final String REGEX = "([a-f0-9]{8})([a-f0-9]{4})([a-f0-9]{4})([a-f0-9]{4})([a-f0-9]{12})";
    private static final String REPLACE = "\\1-\\2-\\3-\\4-\\5";

    public static void normalize(Connection c, MigrationLogger log) throws Exception {
        log.info("=== UUID Normalization: 32-char → 36-char ===");

        // Step 0: Backup all tables that have CHAR(36) columns (README line 169)
        log.info("  Backing up tables with CHAR(36) columns...");
        List<String> backupTables = new ArrayList<>();
        try (Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(
                 "SELECT DISTINCT table_name FROM information_schema.columns " +
                 "WHERE table_schema='public' AND udt_name='bpchar' AND character_maximum_length=36 " +
                 "AND table_name NOT LIKE 'bak_%'")) {
            while (rs.next()) backupTables.add(rs.getString(1));
        }
        try (Statement s = c.createStatement()) {
            for (String t : backupTables) {
                try {
                    s.execute("CREATE TABLE IF NOT EXISTS bak_" + t + "_20260518 (LIKE " + t + " INCLUDING ALL)");
                    s.execute("INSERT INTO bak_" + t + "_20260518 SELECT * FROM " + t + " ON CONFLICT DO NOTHING");
                } catch (Exception e) {
                    log.warn("  backup " + t + " failed: " + e.getMessage().substring(0, 60));
                }
            }
        }
        log.info("  Backups done.");

        // Step 1: Find all CHAR(36) columns
        int total32 = 0, totalCols = 0, totalJson = 0;
        List<ColInfo> charCols = new ArrayList<>();
        try (Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(
                 "SELECT table_name, column_name FROM information_schema.columns " +
                 "WHERE table_schema='public' AND udt_name='bpchar' AND character_maximum_length=36 " +
                 "AND table_name NOT LIKE 'bak_%' ORDER BY table_name, column_name")) {
            while (rs.next()) charCols.add(new ColInfo(rs.getString(1), rs.getString(2)));
        }

        // Step 2: Fix CHAR(36) columns
        for (ColInfo col : charCols) {
            // Count 32-char UUIDs
            int cnt = 0;
            try (Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery(
                     "SELECT COUNT(*) FROM " + col.table +
                     " WHERE length(trim(\"" + col.col + "\")) = 32")) {
                rs.next(); cnt = rs.getInt(1);
            }
            if (cnt == 0) continue;

            log.info("  " + col.table + "." + col.col + ": " + cnt + " rows to fix");
            try (Statement s = c.createStatement()) {
                s.execute("UPDATE " + col.table + " SET \"" + col.col + "\" = " +
                    "regexp_replace(trim(\"" + col.col + "\"), '" + REGEX + "', '" + REPLACE + "')" +
                    " WHERE length(trim(\"" + col.col + "\")) = 32");
            } catch (Exception e) { log.warn("  FAILED " + col.table + "." + col.col + ": " + e.getMessage()); }
        }

        // Step 2.5: Convert CHAR(36) → UUID type (per-column, handles invalid values gracefully)
        log.info("  Converting CHAR(36) → UUID type...");
        for (ColInfo col : charCols) {
            // Check if still bpchar (not already converted)
            try (Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery(
                     "SELECT udt_name FROM information_schema.columns " +
                     "WHERE table_schema='public' AND table_name='" + col.table +
                     "' AND column_name='" + col.col + "'")) {
                rs.next();
                if (!"bpchar".equals(rs.getString(1))) continue;
            } catch (Exception e) { continue; }

            // Fix known invalid UUIDs: t_hot_products uses non-UUID values, replace with proper UUIDs
            if ("t_hot_products".equals(col.table) && "id".equals(col.col)) {
                try (Statement s = c.createStatement()) {
                    s.execute("UPDATE t_hot_products SET id = gen_random_uuid() " +
                        "WHERE trim(id) !~ '^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$'");
                    int fixed = s.getUpdateCount();
                    if (fixed > 0) log.info("  t_hot_products.id: replaced " + fixed + " invalid UUIDs");
                } catch (Exception e) { log.warn("  t_hot_products.id fix failed: " + e.getMessage()); }
            }

            // Do the ALTER
            try (Statement s = c.createStatement()) {
                s.execute("ALTER TABLE " + col.table + " ALTER COLUMN \"" + col.col +
                    "\" TYPE uuid USING trim(\"" + col.col + "\")::uuid");
                log.info("  " + col.table + "." + col.col + ": char(36) → uuid");
            } catch (Exception e) {
                log.warn("  SKIP " + col.table + "." + col.col + ": " +
                    e.getMessage().substring(0, Math.min(60, e.getMessage().length())));
            }
        }

        // Step 3: Find JSON/JSONB columns
        List<ColInfo> jsonCols = new ArrayList<>();
        try (Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(
                 "SELECT table_name, column_name, udt_name FROM information_schema.columns " +
                 "WHERE table_schema='public' AND udt_name IN ('json','jsonb') " +
                 "AND table_name NOT LIKE 'bak_%' ORDER BY table_name, column_name")) {
            while (rs.next()) jsonCols.add(new ColInfo(rs.getString(1), rs.getString(2)));
        }

        // Step 4: Fix JSON/JSONB columns (32-char hex strings inside)
        for (ColInfo col : jsonCols) {
            int cnt = 0;
            try (Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery(
                     "SELECT COUNT(*) FROM " + col.table +
                     " WHERE \"" + col.col + "\"::text ~ '\"[a-f0-9]{32}\"'")) {
                rs.next(); cnt = rs.getInt(1);
            }
            if (cnt == 0) continue;

            log.info("  " + col.table + "." + col.col + " (json): " + cnt + " rows to fix");
            try (Statement s = c.createStatement()) {
                s.execute("UPDATE " + col.table + " SET \"" + col.col + "\" = " +
                    "regexp_replace(\"" + col.col + "\"::text, '" + REGEX + "', '" + REPLACE + "', 'g')::jsonb " +
                    "WHERE \"" + col.col + "\"::text ~ '\"[a-f0-9]{32}\"'");
            } catch (Exception e) { log.warn("  FAILED " + col.table + "." + col.col + ": " + e.getMessage()); }
        }

        log.info("=== UUID Normalization DONE ===");
    }

    private static class ColInfo {
        final String table, col;
        ColInfo(String t, String c) { table = t; col = c; }
    }
}
