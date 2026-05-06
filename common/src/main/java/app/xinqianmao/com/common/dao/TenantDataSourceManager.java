/**
 * File: TenantDataSourceManager.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.common.dao;

import app.xinqianmao.com.common.constant.GlobalConstants;
import com.alibaba.druid.pool.DruidDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages tenant DataSource lifecycle.
 * - Looks up tenant DB connection info from mypet_config
 * - Creates/clones tenant DB from mypet_empty template on first access
 * - Caches DruidDataSource instances per tenant
 */
@Slf4j
@Component
public class TenantDataSourceManager {

    private final Map<String, DruidDataSource> dataSourceCache = new ConcurrentHashMap<>();

    private final String host;
    private final int port;
    private final String user;
    private final String password;

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
     * On first access, clones mypet_empty to create the tenant DB if it doesn't exist.
     */
    public DataSource getOrCreateTenantDataSource(String tenantCode) {
        return dataSourceCache.computeIfAbsent(tenantCode, this::createTenantDataSource);
    }

    private DruidDataSource createTenantDataSource(String tenantCode) {
        String dbName = "mypet_" + tenantCode;
        ensureTenantDatabaseExists(dbName);
        return createDataSource(dbName);
    }

    /**
     * Ensure the tenant database exists. If not, clone from mypet_empty.
     */
    private void ensureTenantDatabaseExists(String dbName) {
        String checkSql = "SELECT 1 FROM pg_database WHERE datname = '" + dbName + "'";
        String createSql = "CREATE DATABASE \"" + dbName + "\" TEMPLATE mypet_empty";

        try (Connection conn = configDataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(checkSql);
            if (rs.next()) {
                log.info("Tenant database '{}' already exists", dbName);
                return;
            }
        } catch (Exception e) {
            log.warn("Error checking database existence for '{}': {}", dbName, e.getMessage());
        }

        // Database doesn't exist — clone from template
        // We need a connection to the default 'postgres' database for CREATE DATABASE
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
     * Create a DruidDataSource for a given database name.
     */
    private DruidDataSource createDataSource(String dbName) {
        DruidDataSource ds = new DruidDataSource();
        ds.setUrl("jdbc:postgresql://" + host + ":" + port + "/" + dbName);
        ds.setUsername(user);
        ds.setPassword(password);
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
