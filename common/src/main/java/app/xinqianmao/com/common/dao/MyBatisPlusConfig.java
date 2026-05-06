/**
 * File: MyBatisPlusConfig.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.common.dao;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus configuration.
 * Configures pagination plugin for PostgreSQL.
 * MapperScan covers all module mapper packages.
 */
@Configuration
@MapperScan(basePackages = {
        "app.xinqianmao.com.admin.dao",
        "app.xinqianmao.com.frontend.dao",
        "app.xinqianmao.com.common.dao"
})
public class MyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.POSTGRE_SQL);
        pagination.setMaxLimit(500L);
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }

}
