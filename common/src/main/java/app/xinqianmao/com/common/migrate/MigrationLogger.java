/**
 * File: MigrationLogger.java
 * Author: system
 * Date: 2026-05-18
 *
 * Logging interface for migration progress. Implementations can write to console,
 * database (c_migration_log), or both.
 */
package app.xinqianmao.com.common.migrate;

public interface MigrationLogger {
    void info(String msg);
    void warn(String msg);
    void error(String msg);
}

