/**
 * File: WebMvcConfig.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.common.web;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Register interceptors.
 * TenantInterceptor runs first (sets DataSource), then AuthInterceptor (validates JWT).
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final TenantInterceptor tenantInterceptor;
    private final AuthInterceptor authInterceptor;

    /** API path patterns that require tenant + auth headers */
    private static final String[] PROTECTED_PATHS = {
            "/admin/**", "/member/**", "/home/**", "/category/**", "/goods/**"
    };

    /** Login paths excluded from auth */
    private static final String[] AUTH_EXCLUDE_PATHS = {
            "/admin/login", "/member/login/**"
    };

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Tenant interceptor: required on all API paths
        registry.addInterceptor(tenantInterceptor)
                .addPathPatterns(PROTECTED_PATHS)
                .order(1);

        // Auth interceptor: required on all API paths except login
        registry.addInterceptor(authInterceptor)
                .addPathPatterns(PROTECTED_PATHS)
                .excludePathPatterns(AUTH_EXCLUDE_PATHS)
                .order(2);
    }
}
