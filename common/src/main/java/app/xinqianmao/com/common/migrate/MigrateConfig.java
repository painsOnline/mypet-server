/**
 * File: MigrateConfig.java
 * Author: system
 * Date: 2026-05-18
 *
 * Migration configuration constants.
 * In production these come from application.properties / Druid DataSource.
 */
package app.xinqianmao.com.common.migrate;

public final class MigrateConfig {
    private MigrateConfig() {}

    public static final String URL_PREFIX = "jdbc:postgresql://127.0.0.1:1800/";
    public static final String CONFIG_DB = "mypet_config";
    public static final String USER = "postgres";
    public static final String PASSWORD = "mypg123abc";

    /** Tenant query: reads active tenant codes from config DB */
    public static final String TENANT_QUERY = "SELECT code FROM c_tenant WHERE is_disable = 0";

    /** Backup date suffix used in bak_ table names */
    public static final String BACKUP_SUFFIX = "_20260516";
}
