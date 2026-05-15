/**
 * File: ConfigDataSourceConfig.java
 * Author: system
 * Date: 2026-05-15
 */
package app.xinqianmao.com.admin.common.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Pooled DataSource for the mypet_config database.
 * Shared by LoginSecurityService and MigrationRunnerService.
 */
@Configuration
public class ConfigDataSourceConfig {

    @Bean(name = "configDataSource")
    public DataSource configDataSource(
            @Value("${mypet.db.host:127.0.0.1}") String host,
            @Value("${mypet.db.port:1800}") String port,
            @Value("${mypet.db.user:postgres}") String user,
            @Value("${mypet.db.password:mypg123abc}") String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/mypet_config");
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(5000);
        config.setIdleTimeout(60000);
        config.setPoolName("config-db-pool");
        return new HikariDataSource(config);
    }
}
