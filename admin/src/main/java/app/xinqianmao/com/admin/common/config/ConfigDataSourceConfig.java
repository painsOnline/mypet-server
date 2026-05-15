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
 * Pooled DataSources for mypet_config and mypet_empty databases.
 * configDataSource  — login security + migration log
 * templateDataSource — mypet_empty template (for tenant migration sync)
 */
@Configuration
public class ConfigDataSourceConfig {

    @Bean(name = "configDataSource")
    public DataSource configDataSource(
            @Value("${mypet.db.host:127.0.0.1}") String host,
            @Value("${mypet.db.port:1800}") String port,
            @Value("${mypet.db.user:postgres}") String user,
            @Value("${mypet.db.password:mypg123abc}") String password) {
        return buildDataSource(host, port, user, password, "mypet_config", "config-db-pool");
    }

    @Bean(name = "templateDataSource")
    public DataSource templateDataSource(
            @Value("${mypet.db.host:127.0.0.1}") String host,
            @Value("${mypet.db.port:1800}") String port,
            @Value("${mypet.db.user:postgres}") String user,
            @Value("${mypet.db.password:mypg123abc}") String password) {
        return buildDataSource(host, port, user, password, "mypet_empty", "template-db-pool");
    }

    private DataSource buildDataSource(String host, String port, String user, String password, String db, String poolName) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + db);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(3);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(5000);
        config.setIdleTimeout(60000);
        config.setPoolName(poolName);
        return new HikariDataSource(config);
    }
}
