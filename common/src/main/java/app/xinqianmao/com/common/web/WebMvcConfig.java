/**
 * File: WebMvcConfig.java
 * Author: system
 * Date: 2026-05-03
 *
 * Register interceptors and static resource mappings.
 * TenantInterceptor runs first (sets DataSource), then AuthInterceptor (validates JWT).
 */
package app.xinqianmao.com.common.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final TenantInterceptor tenantInterceptor;
    private final AuthInterceptor authInterceptor;
    private final String uploadPath;

    public WebMvcConfig(TenantInterceptor tenantInterceptor, AuthInterceptor authInterceptor,
                        @Value("${mypet.upload.path:F:/MyWorkspace/project/mypet/uploads}") String uploadPath) {
        this.tenantInterceptor = tenantInterceptor;
        this.authInterceptor = authInterceptor;
        this.uploadPath = uploadPath;
    }

    /** API path patterns that require tenant + auth headers */
    private static final String[] PROTECTED_PATHS = {
            "/admin/**", "/frontend/**"
    };

    /** Login paths excluded from auth */
    private static final String[] AUTH_EXCLUDE_PATHS = {
            "/admin/login", "/frontend/member/login/**",
            "/frontend/shop/list", "/frontend/shop/validate"
    };

    /** Paths excluded from tenant header requirement (called before login) */
    private static final String[] TENANT_EXCLUDE_PATHS = {
            "/admin/captcha", "/frontend/shop/list", "/frontend/shop/validate"
    };

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(uploadPath).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Tenant interceptor: required on all API paths except captcha (called before login)
        registry.addInterceptor(tenantInterceptor)
                .addPathPatterns(PROTECTED_PATHS)
                .excludePathPatterns(TENANT_EXCLUDE_PATHS)
                .order(1);

        // Auth interceptor: required on all API paths except login
        registry.addInterceptor(authInterceptor)
                .addPathPatterns(PROTECTED_PATHS)
                .excludePathPatterns(AUTH_EXCLUDE_PATHS)
                .order(2);
    }
}
