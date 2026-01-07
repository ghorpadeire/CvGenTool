/**
 * CvGeneratorApplication.java
 *
 * Main entry point for the CV Generator Spring Boot application.
 * This class bootstraps the entire application, initializing:
 * - Spring IoC container (dependency injection)
 * - Embedded Tomcat server
 * - Auto-configuration for all starters
 * - Component scanning for beans
 *
 * @author Pranav Ghorpade
 * @version 1.0
 * @since 2025-01-XX
 *
 * Interview Note:
 * The @SpringBootApplication annotation is a convenience annotation that combines:
 * - @Configuration: Marks this as a source of bean definitions
 * - @EnableAutoConfiguration: Enables Spring Boot's auto-configuration
 * - @ComponentScan: Scans for components in this package and sub-packages
 */
package com.pranav.cvgenerator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for the CV Generator.
 *
 * This Spring Boot application provides:
 * 1. REST API for CV generation using Claude Sonnet 4
 * 2. Web UI for pasting job descriptions
 * 3. LaTeX to PDF compilation
 * 4. Download endpoints for generated CVs
 *
 * Architecture Overview:
 * - Controllers: Handle HTTP requests (REST + Thymeleaf)
 * - Services: Business logic (Claude API, PDF generation)
 * - Repositories: Data persistence (H2 database)
 * - Models: Data structures (DTOs, entities)
 */
@SpringBootApplication
public class CvGeneratorApplication {

    /**
     * Application entry point.
     *
     * This method:
     * 1. Initializes the Spring ApplicationContext
     * 2. Performs auto-configuration based on classpath
     * 3. Starts the embedded Tomcat server on configured port
     * 4. Begins accepting HTTP requests
     *
     * @param args Command-line arguments (can include --server.port=XXXX)
     *
     * Interview Note:
     * SpringApplication.run() is the heart of Spring Boot startup.
     * It handles all the complex initialization that previously required
     * extensive XML configuration in traditional Spring applications.
     */
    public static void main(String[] args) {
        // Print startup banner for visibility
        System.out.println("========================================");
        System.out.println("   CV Generator - Starting Application  ");
        System.out.println("   Powered by Claude Sonnet 4 API       ");
        System.out.println("   Author: Pranav Ghorpade              ");
        System.out.println("========================================");

        // Bootstrap the Spring Boot application
        // This single line does all the heavy lifting:
        // - Creates ApplicationContext
        // - Configures beans based on classpath
        // - Starts embedded server
        SpringApplication.run(CvGeneratorApplication.class, args);

        // Log successful startup
        System.out.println("========================================");
        System.out.println("   Application started successfully!    ");
        System.out.println("   Access at: http://localhost:8055     ");
        System.out.println("========================================");
    }
}
