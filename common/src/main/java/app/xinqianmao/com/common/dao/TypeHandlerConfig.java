/**
 * File: TypeHandlerConfig.java
 * Author: system
 * Date: 2026-05-04
 *
 * Registers custom MyBatis type handlers globally.
 */
package app.xinqianmao.com.common.dao;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class TypeHandlerConfig {

    private final SqlSessionFactory sqlSessionFactory;

    @PostConstruct
    public void registerTypeHandlers() {
        sqlSessionFactory.getConfiguration()
                .getTypeHandlerRegistry()
                .register(new ListStringTypeHandler());
    }
}
