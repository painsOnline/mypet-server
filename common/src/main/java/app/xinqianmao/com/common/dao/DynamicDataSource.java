/**
 * File: DynamicDataSource.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.common.dao;

import app.xinqianmao.com.common.auth.TenantContext;
import app.xinqianmao.com.common.constant.GlobalConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Dynamic DataSource routing based on TenantContext.
 * Maintains its own cache of tenant DataSources without relying on reflection.
 */
@Slf4j
public class DynamicDataSource extends AbstractRoutingDataSource {

    private TenantDataSourceManager tenantDataSourceManager;
    private final ConcurrentMap<String, DataSource> tenantDataSources = new ConcurrentHashMap<>();

    @Override
    protected Object determineCurrentLookupKey() {
        String tenantCode = TenantContext.get();
        if (tenantCode != null && !tenantCode.isBlank()) {
            return tenantCode;
        }
        return GlobalConstants.CONFIG_DB_KEY;
    }

    @Override
    protected DataSource determineTargetDataSource() {
        Object lookupKey = determineCurrentLookupKey();
        log.debug("DynamicDataSource routing to: {}", lookupKey);

        if (lookupKey == null || GlobalConstants.CONFIG_DB_KEY.equals(lookupKey)) {
            return getResolvedDefaultDataSource();
        }

        String tenantCode = (String) lookupKey;
        DataSource ds = tenantDataSources.computeIfAbsent(tenantCode, code -> {
            log.info("Creating DataSource for tenant: {}", code);
            return tenantDataSourceManager.getOrCreateTenantDataSource(code);
        });

        if (ds != null) {
            return ds;
        }

        return getResolvedDefaultDataSource();
    }

    public void setTenantDataSourceManager(TenantDataSourceManager manager) {
        this.tenantDataSourceManager = manager;
    }
}
