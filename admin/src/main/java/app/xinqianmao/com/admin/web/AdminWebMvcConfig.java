/**
 * File: AdminWebMvcConfig.java
 * Author: system
 * Date: 2026-05-04
 *
 * Admin-specific Spring MVC configuration.
 * Maps /uploads/** to serve uploaded image files.
 */
package app.xinqianmao.com.admin.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class AdminWebMvcConfig implements WebMvcConfigurer {

    private final String uploadPath;

    public AdminWebMvcConfig(@Value("${mypet.upload.path:uploads}") String uploadPath) {
        this.uploadPath = uploadPath;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(uploadPath).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}
