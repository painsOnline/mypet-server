/**
 * File: AdminApplication.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.admin.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Admin management backend Spring Boot application entry point.
 */
@SpringBootApplication(scanBasePackages = "app.xinqianmao.com")
public class AdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
    }
}
