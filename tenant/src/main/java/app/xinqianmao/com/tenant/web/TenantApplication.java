/**
 * File: TenantApplication.java
 * Author: system
 * Date: 2026-05-21
 */
package app.xinqianmao.com.tenant.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication
@ComponentScan(
    basePackages = { "app.xinqianmao.com.tenant", "app.xinqianmao.com.common" },
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.REGEX,
            pattern = "app\\.xinqianmao\\.com\\.common\\.web\\.WebMvcConfig"),
        @ComponentScan.Filter(type = FilterType.REGEX,
            pattern = "app\\.xinqianmao\\.com\\.common\\.DbInitializer"),
        @ComponentScan.Filter(type = FilterType.REGEX,
            pattern = "app\\.xinqianmao\\.com\\.common\\.entity\\.Tenant")
    }
)
public class TenantApplication {

    public static void main(String[] args) {
        SpringApplication.run(TenantApplication.class, args);
    }
}
