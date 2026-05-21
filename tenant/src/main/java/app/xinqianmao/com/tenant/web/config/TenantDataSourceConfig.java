/**
 * File: TenantDataSourceConfig.java
 * Author: system
 * Date: 2026-05-21
 *
 * DataSource and security service beans for the tenant management module.
 * All operations use the config database directly (mypet_config).
 */
package app.xinqianmao.com.tenant.web.config;

import app.xinqianmao.com.common.service.CaptchaService;
import app.xinqianmao.com.common.service.LoginSecurityService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class TenantDataSourceConfig {

    @Bean(name = "tenantConfigDataSource")
    public DataSource configDataSource(
            @Value("${mypet.db.host:127.0.0.1}") String host,
            @Value("${mypet.db.port:1800}") String port,
            @Value("${mypet.db.user:postgres}") String user,
            @Value("${mypet.db.password:mypg123abc}") String password) {
        return buildDataSource(host, port, user, password, "mypet_config", "tenant-config-pool");
    }

    @Bean(name = "tenantTemplateDataSource")
    public DataSource templateDataSource(
            @Value("${mypet.db.host:127.0.0.1}") String host,
            @Value("${mypet.db.port:1800}") String port,
            @Value("${mypet.db.user:postgres}") String user,
            @Value("${mypet.db.password:mypg123abc}") String password) {
        return buildDataSource(host, port, user, password, "mypet_empty", "tenant-template-pool");
    }

    /**
     * Login security — config DB tables no longer have tenant_code column.
     */
    @Bean
    public LoginSecurityService loginSecurityService(
            @org.springframework.beans.factory.annotation.Qualifier("tenantConfigDataSource") DataSource dataSource) {
        return new LoginSecurityService(dataSource, false);
    }

    @Bean
    public CaptchaService captchaService() {
        return new CaptchaService();
    }

    private DataSource buildDataSource(String host, String port, String user, String password,
                                       String db, String poolName) {
        HikariConfig c = new HikariConfig();
        c.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + db);
        c.setUsername(user);
        c.setPassword(password);
        c.setMaximumPoolSize(3);
        c.setMinimumIdle(1);
        c.setConnectionTimeout(5000);
        c.setIdleTimeout(60000);
        c.setPoolName(poolName);
        return new HikariDataSource(c);
    }
}
