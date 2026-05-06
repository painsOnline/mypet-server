/**
 * File: DataSourceConfig.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.common.dao;

import app.xinqianmao.com.common.constant.GlobalConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * DataSource configuration for SaaS multi-tenant routing.
 * Registers DynamicDataSource as the primary DataSource for MyBatis-Plus.
 * The config DB is always available. Tenant DBs are lazily created on first access.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataSourceConfig {

    private final TenantDataSourceManager tenantDataSourceManager;

    /**
     * Primary DataSource bean — DynamicDataSource that routes to the correct tenant DB.
     */
    @Bean
    @Primary
    public DataSource dataSource() {
        DynamicDataSource dynamicDataSource = new DynamicDataSource();
        dynamicDataSource.setTenantDataSourceManager(tenantDataSourceManager);

        // Target DataSources: config DB is always present
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(GlobalConstants.CONFIG_DB_KEY, tenantDataSourceManager.getConfigDataSource());

        dynamicDataSource.setTargetDataSources(targetDataSources);
        dynamicDataSource.setDefaultTargetDataSource(tenantDataSourceManager.getConfigDataSource());

        dynamicDataSource.afterPropertiesSet();
        return dynamicDataSource;
    }
}
