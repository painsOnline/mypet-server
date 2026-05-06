/**
 * File: CommonAutoConfiguration.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.common.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Spring Boot auto-configuration entry point for the common module.
 * ComponentScan ensures all @Component, @Configuration classes in common are discovered.
 */
@AutoConfiguration
@ComponentScan(basePackages = "app.xinqianmao.com.common")
public class CommonAutoConfiguration {
}
