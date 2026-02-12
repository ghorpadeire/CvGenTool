/**
 * AppConfig.java
 *
 * General application configuration class.
 * Provides beans and settings for various application components.
 *
 * @author Pranav Ghorpade
 */
package com.pranav.cvgenerator.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.Executor;

/**
 * Application-wide configuration.
 *
 * Provides:
 * - JSON ObjectMapper configuration
 * - Async task executor for background processing
 * - LaTeX compiler WebClient
 * - Cache settings
 */
@Configuration
@EnableAsync
public class AppConfig {

    /**
     * LaTeX compilation API URL.
     * Using YtoTech's free LaTeX service for PDF generation.
     */
    @Value("${latex.api.url:https://latex.ytotech.com/builds/sync}")
    private String latexApiUrl;

    /**
     * LaTeX API timeout in milliseconds.
     * LaTeX compilation can be slow, so we use a longer timeout.
     */
    @Value("${latex.api.timeout:60000}")
    private int latexTimeout;

    /**
     * Cache time-to-live in milliseconds.
     * Default: 24 hours
     */
    @Value("${cache.cv.ttl:86400000}")
    private long cacheTtl;

    /**
     * Maximum cache entries.
     * Default: 100
     */
    @Value("${cache.cv.max-size:100}")
    private int cacheMaxSize;

    /**
     * Creates a configured Jackson ObjectMapper.
     *
     * Configuration:
     * - Java 8 date/time support (JSR-310)
     * - Pretty printing disabled for production
     * - Fail on unknown properties disabled
     *
     * @return Configured ObjectMapper instance
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Register Java 8 date/time module for LocalDateTime support
        mapper.registerModule(new JavaTimeModule());

        // Disable writing dates as timestamps (use ISO-8601 format)
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Don't fail on unknown properties (resilient parsing)
        mapper.configure(
            com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
            false
        );

        return mapper;
    }

    /**
     * Creates a WebClient for LaTeX compilation API.
     *
     * The YtoTech LaTeX API accepts POST requests with:
     * - LaTeX source code
     * - Compiler selection (pdflatex)
     * - Resource files (fonts, etc.)
     *
     * @return Configured WebClient for LaTeX API
     */
    @Bean(name = "latexWebClient")
    public WebClient latexWebClient() {
        return WebClient.builder()
                .baseUrl(latexApiUrl)
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(50 * 1024 * 1024)) // 50MB for PDF response
                .build();
    }

    /**
     * Creates an async task executor for background CV generation.
     *
     * Configuration:
     * - Core pool size: 2 (handles concurrent generations)
     * - Max pool size: 4 (allows burst capacity)
     * - Queue capacity: 100 (buffers requests during high load)
     *
     * @return Configured TaskExecutor for async operations
     */
    @Bean(name = "cvGenerationExecutor")
    public Executor cvGenerationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Core threads - always running
        executor.setCorePoolSize(2);

        // Maximum threads - created when queue is full
        executor.setMaxPoolSize(4);

        // Queue capacity - requests wait here when all threads busy
        executor.setQueueCapacity(100);

        // Thread naming for debugging
        executor.setThreadNamePrefix("cv-gen-");

        // Initialize the executor
        executor.initialize();

        return executor;
    }

    // ==================== GETTERS ====================

    /**
     * Gets the LaTeX API URL.
     *
     * @return LaTeX compilation API URL
     */
    public String getLatexApiUrl() {
        return latexApiUrl;
    }

    /**
     * Gets the LaTeX API timeout.
     *
     * @return Timeout in milliseconds
     */
    public int getLatexTimeout() {
        return latexTimeout;
    }

    /**
     * Gets the cache TTL.
     *
     * @return Cache time-to-live in milliseconds
     */
    public long getCacheTtl() {
        return cacheTtl;
    }

    /**
     * Gets the maximum cache size.
     *
     * @return Maximum number of cached entries
     */
    public int getCacheMaxSize() {
        return cacheMaxSize;
    }
}
