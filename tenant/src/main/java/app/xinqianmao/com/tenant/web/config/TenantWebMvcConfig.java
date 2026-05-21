/**
 * File: TenantWebMvcConfig.java
 * Author: system
 * Date: 2026-05-21
 *
 * Register interceptors for the tenant management module (/tenant/** paths).
 * No Tenant header required — operates directly on config DB.
 * AuthInterceptor validates JWT for all /tenant/** paths except login and captcha.
 */
package app.xinqianmao.com.tenant.web.config;

import app.xinqianmao.com.common.auth.TenantContext;
import app.xinqianmao.com.common.web.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class TenantWebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public TenantWebMvcConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // No TenantInterceptor — tenant module operates on config DB directly.
        // Set TenantContext to "config" so any code that reads it gets a valid value.
        registry.addInterceptor(new TenantContextBootstrapInterceptor())
                .addPathPatterns("/tenant/**")
                .order(1);

        // Auth interceptor for all /tenant/** paths except login and captcha
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/tenant/**")
                .excludePathPatterns("/tenant/login", "/tenant/captcha")
                .order(2);
    }

    /**
     * Sets TenantContext to "config" for all tenant management requests
     * so that downstream code that reads TenantContext gets a valid value.
     */
    static class TenantContextBootstrapInterceptor implements org.springframework.web.servlet.HandlerInterceptor {
        @Override
        public boolean preHandle(jakarta.servlet.http.HttpServletRequest request,
                                 jakarta.servlet.http.HttpServletResponse response,
                                 Object handler) {
            TenantContext.set("config");
            return true;
        }

        @Override
        public void afterCompletion(jakarta.servlet.http.HttpServletRequest request,
                                    jakarta.servlet.http.HttpServletResponse response,
                                    Object handler, Exception ex) {
            TenantContext.clear();
        }
    }
}
