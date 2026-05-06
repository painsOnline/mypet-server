/**
 * File: FrontendApplication.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.frontend.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Mini-program user API Spring Boot application entry point.
 */
@SpringBootApplication(scanBasePackages = "app.xinqianmao.com")
public class FrontendApplication {

    public static void main(String[] args) {
        SpringApplication.run(FrontendApplication.class, args);
    }
}
