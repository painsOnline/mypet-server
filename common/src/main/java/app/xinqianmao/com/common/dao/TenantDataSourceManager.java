/**
 * File: TenantDataSourceManager.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.common.dao;

import app.xinqianmao.com.common.utils.CryptoUtil;
import com.alibaba.druid.pool.DruidDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Manages tenant DataSource lifecycle.
 * - Looks up tenant DB connection info from mypet_config
 * - Creates/clones tenant DB from mypet_empty template on first access
 * - Caches DruidDataSource instances per tenant
 * - Decrypts DB passwords from c_database_instance using JWT secret
 */
@Slf4j
@Component
public class TenantDataSourceManager {

    private final Map<String, DruidDataSource> dataSourceCache = new ConcurrentHashMap<>();

    private final String host;
    private final int port;
    private final String user;
    private final String password;

    /** Encryption/decryption key for DB passwords in c_database_instance. Set once and never change. */
    @Value("${mypet.db.encrypt-key:mypet-jwt-secret-key-2026-minimum-32chars!!}")
    private String encryptKey;

    /** Config DB DataSource — used to query tenant registry */
    private final DruidDataSource configDataSource;

    public TenantDataSourceManager(
            @Value("${mypet.db.host:127.0.0.1}") String host,
            @Value("${mypet.db.port:1800}") int port,
            @Value("${mypet.db.user:postgres}") String user,
            @Value("${mypet.db.password:mypg123abc}") String password) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;

        this.configDataSource = createDataSource("mypet_config");
        log.info("Config DataSource initialized: mypet_config");
    }

    /**
     * Get or create DataSource for a tenant.
     * Validates tenant code against c_tenant, then creates/clones the tenant DB.
     */
    public DataSource getOrCreateTenantDataSource(String tenantCode) {
        // Validate tenant exists and is not disabled
        lookupTenant(tenantCode);
        return dataSourceCache.computeIfAbsent(tenantCode, this::createTenantDataSource);
    }

    /** Tenant code must match this pattern (alphanumeric + underscore + hyphen) */
    private static final Pattern TENANT_CODE_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,64}$");

    /**
     * Look up tenant by code from c_tenant in config DB.
     * Validates the tenant exists and is not disabled.
     * Disabled tenants (is_disable=1) cannot log into the admin backend.
     */
    private void lookupTenant(String tenantCode) {
        // Validate tenantCode format before any DB access
        if (!TENANT_CODE_PATTERN.matcher(tenantCode).matches()) {
            throw new RuntimeException("Invalid tenant code format: '" +
                    tenantCode.replaceAll("[\r\n]", "") + "'");
        }
        String sql = "SELECT code, name, is_disable FROM c_tenant WHERE code = ?";
        try (Connection conn = configDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tenantCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int disabled = rs.getInt("is_disable");
                    if (disabled == 1) {
                        throw new RuntimeException("Tenant '" + tenantCode + "' has been disabled");
                    }
                    log.info("Tenant '{}' ({}) validated", tenantCode, rs.getString("name"));
                } else {
                    throw new RuntimeException("Unknown tenant: '" + tenantCode +
                            "'. Please check the Tenant header.");
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to look up tenant '{}'", tenantCode, e);
            throw new RuntimeException("Cannot validate tenant: " + tenantCode, e);
        }
    }

    /**
     * Get the decrypted database password for a tenant from c_database_instance.
     * Joins c_tenant → c_database_instance to find the encrypted password,
     * then decrypts it using the JWT secret.
     */
    private String getDecryptedDbPassword(String tenantCode) {
        String sql = "SELECT di.\"password\" FROM c_database_instance di " +
                     "JOIN c_tenant t ON t.database_instance_id = di.id " +
                     "WHERE t.code = ?";
        try (Connection conn = configDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tenantCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String encryptedPassword = rs.getString("password");
                    return CryptoUtil.decrypt(encryptedPassword, encryptKey);
                }
            }
        } catch (Exception e) {
            log.error("Failed to get DB password for tenant '{}'", tenantCode, e);
        }
        throw new RuntimeException("Cannot get database password for tenant: " + tenantCode);
    }

    private DruidDataSource createTenantDataSource(String tenantCode) {
        String dbName = "mypet_" + tenantCode;
        ensureTenantDatabaseExists(dbName);
        String decryptedPassword = getDecryptedDbPassword(tenantCode);
        return createDataSource(dbName, decryptedPassword);
    }

    /** Database name pattern: mypet_ followed by tenant code (alphanumeric + underscore + hyphen) */
    private static final Pattern DB_NAME_PATTERN = Pattern.compile("^mypet_[a-zA-Z0-9_-]{1,64}$");

    /**
     * Ensure the tenant database exists. If not, clone from mypet_empty.
     */
    private void ensureTenantDatabaseExists(String dbName) {
        // Validate dbName format before any DB operation
        if (!DB_NAME_PATTERN.matcher(dbName).matches()) {
            throw new RuntimeException("Invalid database name: '" +
                    dbName.replaceAll("[\r\n]", "") + "'");
        }

        // PreparedStatement works for SELECT on pg_database
        String checkSql = "SELECT 1 FROM pg_database WHERE datname = ?";
        try (Connection conn = configDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setString(1, dbName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    log.info("Tenant database '{}' already exists", dbName);
                    return;
                }
            }
        } catch (Exception e) {
            log.warn("Error checking database existence for '{}': {}", dbName, e.getMessage());
        }

        // Database doesn't exist — clone from template.
        // dbName already validated by DB_NAME_PATTERN above, safe for DDL.
        // DDL does not support PreparedStatement in PostgreSQL.
        String createSql = "CREATE DATABASE \"" + dbName + "\" TEMPLATE mypet_empty";
        DruidDataSource postgresDs = createDataSource("postgres");
        try (Connection conn = postgresDs.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createSql);
            log.info("Tenant database '{}' created from template mypet_empty", dbName);
        } catch (Exception e) {
            log.error("Failed to create tenant database '{}'", dbName, e);
            throw new RuntimeException("Cannot create tenant database: " + dbName, e);
        } finally {
            postgresDs.close();
        }
    }

    /**
     * Create a DruidDataSource for a given database name with explicit password.
     * Used for tenant databases (password decrypted from c_database_instance).
     */
    private DruidDataSource createDataSource(String dbName, String dbPassword) {
        DruidDataSource ds = new DruidDataSource();
        ds.setUrl("jdbc:postgresql://" + host + ":" + port + "/" + dbName + "?stringtype=unspecified");
        ds.setUsername(user);
        ds.setPassword(dbPassword);
        ds.setDriverClassName("org.postgresql.Driver");

        // Pool settings
        ds.setInitialSize(2);
        ds.setMinIdle(2);
        ds.setMaxActive(10);
        ds.setMaxWait(5000);
        ds.setValidationQuery("SELECT 1");
        ds.setTestWhileIdle(true);
        ds.setTestOnBorrow(false);
        ds.setTestOnReturn(false);
        ds.setTimeBetweenEvictionRunsMillis(60000);
        ds.setMinEvictableIdleTimeMillis(300000);
        return ds;
    }

    /**
     * Create a DruidDataSource for a given database name using default password.
     * Used for config database bootstrap (mypet_config) and postgres connections.
     */
    private DruidDataSource createDataSource(String dbName) {
        return createDataSource(dbName, this.password);
    }

    /**
     * Get the config DataSource (used for queries against mypet_config).
     */
    public DataSource getConfigDataSource() {
        return configDataSource;
    }

    /**
     * Get all currently loaded tenant codes.
     */
    public java.util.Set<String> getLoadedTenants() {
        return dataSourceCache.keySet();
    }
}
