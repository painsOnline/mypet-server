/**
 * File: TenantService.java
 * Author: system
 * Date: 2026-05-21
 */
package app.xinqianmao.com.tenant.service;

import app.xinqianmao.com.common.entity.Tenant;
import app.xinqianmao.com.common.exception.BizException;
import app.xinqianmao.com.tenant.dao.ConfigTenantMapper;
import app.xinqianmao.com.common.utils.DateTimeUtil;
import app.xinqianmao.com.tenant.common.entity.DatabaseInstance;
import app.xinqianmao.com.tenant.common.pojo.TenantListResponse;
import app.xinqianmao.com.tenant.common.pojo.TenantSaveRequest;
import app.xinqianmao.com.tenant.dao.DatabaseInstanceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Tenant management service.
 * Handles CRUD for c_tenant and lifecycle of per-tenant PostgreSQL databases.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {

    private final ConfigTenantMapper configTenantMapper;
    private final DatabaseInstanceMapper databaseInstanceMapper;

    @Qualifier("tenantConfigDataSource")
    private final DataSource configDataSource;

    @Qualifier("tenantTemplateDataSource")
    private final DataSource templateDataSource;

    @Value("${mypet.db.host:127.0.0.1}")
    private String dbHost;

    @Value("${mypet.db.port:1800}")
    private String dbPort;

    @Value("${mypet.db.user:postgres}")
    private String dbUser;

    @Value("${mypet.db.password:mypg123abc}")
    private String dbPassword;

    /** Pattern for tenant code validation (used as database name prefix). */
    private static final Pattern CODE_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,64}$");

    /**
     * List all tenants with database instance name enriched.
     */
    public List<TenantListResponse> listAll() {
        List<Tenant> tenants = configTenantMapper.selectList(new LambdaQueryWrapper<>());
        List<TenantListResponse> result = new ArrayList<>();
        for (Tenant tenant : tenants) {
            result.add(toResponse(tenant));
        }
        return result;
    }

    /**
     * Get a single tenant by ID with database instance name enriched.
     */
    public TenantListResponse getById(String id) {
        Tenant tenant = configTenantMapper.selectById(id);
        if (tenant == null) {
            throw new BizException("404", "租户不存在");
        }
        return toResponse(tenant);
    }

    /**
     * Create a new tenant.
     * 1. Validate tenant code format and uniqueness.
     * 2. Clone the mypet_empty template database to create the tenant's database.
     * 3. Insert the c_tenant record.
     * 4. Return the new tenant.
     */
    public TenantListResponse create(TenantSaveRequest req) {
        // Validate code format
        String code = req.getCode();
        if (!CODE_PATTERN.matcher(code).matches()) {
            throw new BizException("400", "租户code格式不正确，仅允许字母、数字、下划线和连字符，长度1-64");
        }

        // Check code uniqueness
        Long count = configTenantMapper.selectCount(
                new LambdaQueryWrapper<Tenant>().eq(Tenant::getCode, code));
        if (count > 0) {
            throw new BizException("400", "租户code已存在");
        }

        // Validate database instance exists
        DatabaseInstance dbInst = databaseInstanceMapper.selectById(req.getDatabaseInstanceId());
        if (dbInst == null) {
            throw new BizException("400", "数据库实例不存在");
        }

        // Create tenant database by cloning template
        String dbName = "mypet_" + code;
        log.info("Creating tenant database: {}", dbName);
        try (Connection conn = getPostgresConnection();
             Statement stmt = conn.createStatement()) {
            // Terminate other connections to mypet_empty so we can use it as template
            try {
                stmt.execute("SELECT pg_terminate_backend(pid) FROM pg_stat_activity " +
                             "WHERE datname = 'mypet_empty' AND pid <> pg_backend_pid()");
            } catch (SQLException ignored) { /* best effort */ }
            // Drop if exists from a previous failed attempt
            try {
                stmt.execute("DROP DATABASE IF EXISTS \"" + dbName + "\"");
            } catch (SQLException ignored) { /* best effort */ }
            stmt.execute("CREATE DATABASE \"" + dbName + "\" TEMPLATE mypet_empty");
            log.info("Tenant database created: {}", dbName);
        } catch (SQLException e) {
            log.error("Failed to create database {}: {}", dbName, e.getMessage());
            throw new BizException("500", "创建租户数据库失败: " + e.getMessage());
        }

        // Insert c_tenant record
        Tenant tenant = new Tenant();
        tenant.setCode(code);
        tenant.setName(req.getName());
        tenant.setDatabaseInstanceId(req.getDatabaseInstanceId());
        tenant.setIsDisable(req.getIsDisable() != null ? req.getIsDisable() : 0);
        tenant.setIsBussinessOpen(0);
        tenant.setFreeShippingAmount(req.getFreeShippingAmount());
        tenant.setCreateTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
        tenant.setModifyTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
        configTenantMapper.insert(tenant);

        log.info("Tenant created: code={}, id={}", code, tenant.getId());
        return toResponse(tenant);
    }

    /**
     * Update an existing tenant.
     * Only allows updating name, isDisable, isBussinessOpen, freeShippingAmount.
     * Does NOT allow changing code or databaseInstanceId.
     */
    public TenantListResponse update(String id, TenantSaveRequest req) {
        Tenant tenant = configTenantMapper.selectById(id);
        if (tenant == null) {
            throw new BizException("404", "租户不存在");
        }

        tenant.setName(req.getName());
        tenant.setIsDisable(req.getIsDisable());
        tenant.setIsBussinessOpen(req.getIsBussinessOpen());
        tenant.setFreeShippingAmount(req.getFreeShippingAmount());
        tenant.setModifyTime(LocalDateTime.now(DateTimeUtil.ZONE_BEIJING));
        configTenantMapper.updateById(tenant);

        return toResponse(tenant);
    }

    /**
     * Delete a tenant.
     * 1. Validates the code format (used as DB name).
     * 2. Checks the tenant database for existing orders or products.
     * 3. If clean, drops the tenant database and deletes the c_tenant record.
     */
    public void delete(String id) {
        Tenant tenant = configTenantMapper.selectById(id);
        if (tenant == null) {
            throw new BizException("404", "租户不存在");
        }

        String code = tenant.getCode();
        if (!CODE_PATTERN.matcher(code).matches()) {
            throw new BizException("400", "租户code格式不允许，无法安全删除");
        }

        // Check tenant database for orders and products
        String dbName = "mypet_" + code;
        checkTenantDbEmpty(dbName);

        // Drop tenant database
        log.info("Dropping tenant database: {}", dbName);
        try (Connection conn = getPostgresConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP DATABASE IF EXISTS \"" + dbName + "\"");
            log.info("Tenant database dropped: {}", dbName);
        } catch (SQLException e) {
            log.error("Failed to drop database {}: {}", dbName, e.getMessage());
            throw new BizException("500", "删除租户数据库失败: " + e.getMessage());
        }

        // Delete c_tenant record
        configTenantMapper.deleteById(id);
        log.info("Tenant deleted: code={}, id={}", code, id);
    }

    /**
     * List all database instances.
     */
    public List<DatabaseInstance> getDatabaseInstances() {
        return databaseInstanceMapper.selectList(new LambdaQueryWrapper<>());
    }

    /**
     * Convert Tenant entity to TenantListResponse with database instance name.
     */
    private TenantListResponse toResponse(Tenant tenant) {
        TenantListResponse resp = new TenantListResponse();
        resp.setId(tenant.getId());
        resp.setCode(tenant.getCode());
        resp.setName(tenant.getName());
        resp.setDatabaseInstanceId(tenant.getDatabaseInstanceId());
        resp.setIsDisable(tenant.getIsDisable());
        resp.setIsBussinessOpen(tenant.getIsBussinessOpen());
        resp.setFreeShippingAmount(tenant.getFreeShippingAmount());
        resp.setCreateTime(tenant.getCreateTime());
        resp.setModifyTime(tenant.getModifyTime());

        // Enrich with database instance display name
        if (tenant.getDatabaseInstanceId() != null) {
            DatabaseInstance dbInst = databaseInstanceMapper.selectById(tenant.getDatabaseInstanceId());
            if (dbInst != null) {
                resp.setDatabaseInstanceName(dbInst.getHost() + ":" + dbInst.getPort());
            }
        }
        return resp;
    }

    /**
     * Get a JDBC connection to the postgres default database for DDL operations.
     */
    private Connection getPostgresConnection() throws SQLException {
        String url = "jdbc:postgresql://" + dbHost + ":" + dbPort + "/postgres";
        return DriverManager.getConnection(url, dbUser, dbPassword);
    }

    /**
     * Get a JDBC connection to a specific database.
     */
    private Connection getTenantDbConnection(String database) throws SQLException {
        String url = "jdbc:postgresql://" + dbHost + ":" + dbPort + "/" + database;
        return DriverManager.getConnection(url, dbUser, dbPassword);
    }

    /**
     * Check if a tenant database has any active orders or products.
     * Throws BizException if data exists, preventing deletion.
     */
    private void checkTenantDbEmpty(String dbName) {
        try (Connection conn = getTenantDbConnection(dbName)) {
            // Check orders
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM t_order WHERE is_delete = 0")) {
                if (rs.next() && rs.getInt(1) > 0) {
                    throw new BizException("400", "该租户下存在订单或商品，不能删除。建议禁用该租户或将营业状态设为暂停营业。");
                }
            }
            // Check products
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM t_product WHERE is_delete = 0")) {
                if (rs.next() && rs.getInt(1) > 0) {
                    throw new BizException("400", "该租户下存在订单或商品，不能删除。建议禁用该租户或将营业状态设为暂停营业。");
                }
            }
        } catch (BizException e) {
            throw e;
        } catch (SQLException e) {
            // Database might not exist yet or other connection error — treat as empty
            log.warn("Could not check tenant DB {}: {}", dbName, e.getMessage());
        }
    }
}
