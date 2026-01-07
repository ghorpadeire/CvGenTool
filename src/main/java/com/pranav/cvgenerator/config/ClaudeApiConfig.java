/**
 * ClaudeApiConfig.java
 *
 * Configuration class for Claude API integration.
 * Manages API credentials, endpoints, and HTTP client settings.
 *
 * @author Pranav Ghorpade
 * @version 1.0
 * @since 2025-01-XX
 *
 * Interview Note:
 * This class demonstrates Spring's externalized configuration pattern.
 * All sensitive values (API keys) are loaded from environment variables,
 * never hardcoded. This is crucial for security in production.
 */
package com.pranav.cvgenerator.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * Configuration class for Claude API settings.
 *
 * Provides:
 * - API endpoint configuration
 * - Authentication setup
 * - HTTP client configuration
 * - Timeout settings
 *
 * Interview Note:
 * Using @Configuration marks this class as a source of bean definitions.
 * Spring will process this class and register the beans during startup.
 */
@Configuration
public class ClaudeApiConfig {

    /**
     * Claude API key loaded from environment variable.
     * NEVER hardcode API keys - always use externalized configuration.
     *
     * Set via: export CLAUDE_API_KEY=sk-ant-xxxxx (Linux/Mac)
     *          set CLAUDE_API_KEY=sk-ant-xxxxx (Windows CMD)
     *          $env:CLAUDE_API_KEY="sk-ant-xxxxx" (PowerShell)
     */
    @Value("${claude.api.key:}")
    private String apiKey;

    /**
     * Claude API endpoint URL.
     * Default: https://api.anthropic.com/v1/messages
     */
    @Value("${claude.api.url:https://api.anthropic.com/v1/messages}")
    private String apiUrl;

    /**
     * Claude model to use for CV generation.
     * Default: claude-sonnet-4-20250514 (best quality/speed balance)
     */
    @Value("${claude.api.model:claude-sonnet-4-20250514}")
    private String model;

    /**
     * Maximum tokens for Claude response.
     * Default: 8000 (sufficient for LaTeX CV + analysis)
     */
    @Value("${claude.api.max-tokens:8000}")
    private int maxTokens;

    /**
     * Request timeout in milliseconds.
     * Default: 30000 (30 seconds)
     */
    @Value("${claude.api.timeout:30000}")
    private int timeout;

    // ==================== GETTERS ====================

    /**
     * Gets the configured API key.
     *
     * @return The Claude API key
     * @throws IllegalStateException if API key is not configured
     */
    public String getApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                "Claude API key not configured. Set CLAUDE_API_KEY environment variable.");
        }
        return apiKey;
    }

    /**
     * Gets the API URL.
     *
     * @return The Claude API endpoint URL
     */
    public String getApiUrl() {
        return apiUrl;
    }

    /**
     * Gets the configured model.
     *
     * @return The Claude model identifier
     */
    public String getModel() {
        return model;
    }

    /**
     * Gets the maximum tokens setting.
     *
     * @return Maximum tokens for response
     */
    public int getMaxTokens() {
        return maxTokens;
    }

    /**
     * Gets the timeout setting.
     *
     * @return Timeout in milliseconds
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * Creates a configured WebClient for Claude API calls.
     *
     * This WebClient is pre-configured with:
     * - Base URL for Claude API
     * - Required headers (API key, content type, version)
     * - Timeout settings
     * - Connection pooling
     *
     * Interview Note:
     * WebClient is the modern, non-blocking HTTP client in Spring.
     * It's recommended over RestTemplate for new projects.
     * The reactive nature allows better resource utilization under load.
     *
     * @return Configured WebClient instance
     */
    @Bean(name = "claudeWebClient")
    public WebClient claudeWebClient() {
        // Configure HTTP client with timeout
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMillis(timeout));

        // Build WebClient with Claude API configuration
        return WebClient.builder()
                .baseUrl(apiUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                // Set default headers for all requests
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("anthropic-version", "2023-06-01")
                // Note: API key header is set per-request for security
                // This allows key rotation without recreating the client
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024)) // 10MB max response size
                .build();
    }

    /**
     * Checks if the API key is configured.
     *
     * @return true if API key is present and not blank
     */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
