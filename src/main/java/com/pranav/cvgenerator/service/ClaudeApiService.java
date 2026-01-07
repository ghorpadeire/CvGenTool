/**
 * ClaudeApiService.java
 *
 * Service for communicating with Claude API (Anthropic).
 * Handles CV generation requests using Claude Sonnet 4.
 *
 * @author Pranav Ghorpade
 * @version 1.0
 * @since 2025-01-XX
 *
 * Interview Note:
 * This service demonstrates:
 * - RESTful API integration with WebClient
 * - Error handling and retry logic
 * - Response parsing and validation
 * - Proper timeout handling
 */
package com.pranav.cvgenerator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pranav.cvgenerator.config.ClaudeApiConfig;
import com.pranav.cvgenerator.model.ClaudeApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for Claude API integration.
 *
 * Responsibilities:
 * 1. Build API requests with proper headers
 * 2. Send requests to Claude API
 * 3. Handle rate limiting and errors
 * 4. Parse and validate responses
 *
 * Interview Note:
 * The Claude Messages API follows a specific format:
 * - System prompt sets the AI's behavior
 * - User messages contain the task/query
 * - Assistant messages contain responses
 */
@Service
@Slf4j
public class ClaudeApiService {

    /**
     * Anthropic API version header value.
     * Required for all API requests.
     */
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    /**
     * Configuration for API credentials and settings.
     */
    private final ClaudeApiConfig config;

    /**
     * Pre-configured WebClient for API calls.
     */
    private final WebClient webClient;

    /**
     * Jackson ObjectMapper for JSON handling.
     */
    private final ObjectMapper objectMapper;

    /**
     * Constructor with dependency injection.
     *
     * @param config Claude API configuration
     * @param claudeWebClient Configured WebClient for Claude API
     * @param objectMapper JSON parser
     */
    public ClaudeApiService(
            ClaudeApiConfig config,
            @Qualifier("claudeWebClient") WebClient claudeWebClient,
            ObjectMapper objectMapper) {
        this.config = config;
        this.webClient = claudeWebClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Generates a CV using Claude API.
     *
     * Process:
     * 1. Build the API request body
     * 2. Send POST request to Claude Messages API
     * 3. Handle any errors (rate limits, timeouts)
     * 4. Parse the response to extract LaTeX and analysis
     *
     * @param systemPrompt The CV_GEN system prompt defining behavior
     * @param userMessage Combined candidate data + job description
     * @return Parsed Claude API response with LaTeX CV and analysis
     * @throws RuntimeException if API call fails
     */
    public ClaudeApiResponse generateCv(String systemPrompt, String userMessage) {
        log.info("Calling Claude API for CV generation...");
        long startTime = System.currentTimeMillis();

        try {
            // Build the request body
            Map<String, Object> requestBody = buildRequestBody(systemPrompt, userMessage);

            // Make the API call
            String response = webClient.post()
                    .header("x-api-key", config.getApiKey())
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    // Retry on 429 (rate limit) and 529 (overloaded)
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                            .filter(this::isRetryableError)
                            .onRetryExhaustedThrow((spec, signal) ->
                                    signal.failure()))
                    .block(Duration.ofMillis(config.getTimeout()));

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Claude API response received in {}ms", elapsed);

            // Parse and return the response
            return parseResponse(response);

        } catch (WebClientResponseException e) {
            log.error("Claude API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Claude API error: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to call Claude API", e);
            throw new RuntimeException("Failed to generate CV: " + e.getMessage(), e);
        }
    }

    /**
     * Builds the request body for Claude Messages API.
     *
     * Request format:
     * {
     *   "model": "claude-sonnet-4-20250514",
     *   "max_tokens": 8000,
     *   "system": "CV_GEN system prompt...",
     *   "messages": [
     *     {"role": "user", "content": "..."}
     *   ]
     * }
     *
     * @param systemPrompt System prompt for CV_GEN behavior
     * @param userMessage User message with candidate data and JD
     * @return Request body as a Map
     */
    private Map<String, Object> buildRequestBody(String systemPrompt, String userMessage) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", config.getModel());
        body.put("max_tokens", config.getMaxTokens());
        body.put("system", systemPrompt);

        // Messages array with user's request
        Map<String, String> userMessageObj = new HashMap<>();
        userMessageObj.put("role", "user");
        userMessageObj.put("content", userMessage);
        body.put("messages", List.of(userMessageObj));

        return body;
    }

    /**
     * Parses the Claude API response to extract the generated content.
     *
     * Claude's response format:
     * {
     *   "content": [
     *     {"type": "text", "text": "JSON response from CV_GEN"}
     *   ],
     *   "stop_reason": "end_turn",
     *   ...
     * }
     *
     * @param responseBody Raw JSON response from Claude
     * @return Parsed ClaudeApiResponse object
     */
    private ClaudeApiResponse parseResponse(String responseBody) {
        try {
            // First, parse the outer Claude API response
            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode contentArray = rootNode.path("content");

            if (contentArray.isEmpty()) {
                log.error("Empty content in Claude response");
                throw new RuntimeException("Empty response from Claude API");
            }

            // Get the text content from the first content block
            String textContent = contentArray.get(0).path("text").asText();

            // The text content should be a JSON object from CV_GEN
            // Extract JSON from the text (it might be wrapped in markdown code blocks)
            String jsonContent = extractJson(textContent);

            // Parse the CV_GEN JSON response
            return objectMapper.readValue(jsonContent, ClaudeApiResponse.class);

        } catch (JsonProcessingException e) {
            log.error("Failed to parse Claude response: {}", responseBody, e);
            throw new RuntimeException("Failed to parse Claude response", e);
        }
    }

    /**
     * Extracts JSON content from Claude's response text.
     *
     * Claude might wrap JSON in markdown code blocks like:
     * ```json
     * { ... }
     * ```
     *
     * This method handles both raw JSON and code-block-wrapped JSON.
     *
     * @param text The text content from Claude
     * @return Extracted JSON string
     */
    private String extractJson(String text) {
        // Remove markdown code block markers if present
        String cleaned = text.trim();

        // Handle ```json ... ``` format
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }

        // Find the JSON object boundaries
        int jsonStart = cleaned.indexOf('{');
        int jsonEnd = cleaned.lastIndexOf('}');

        if (jsonStart == -1 || jsonEnd == -1 || jsonEnd <= jsonStart) {
            log.error("Could not find JSON in response: {}", text);
            throw new RuntimeException("Invalid JSON in Claude response");
        }

        return cleaned.substring(jsonStart, jsonEnd + 1);
    }

    /**
     * Determines if an error is retryable.
     *
     * Retryable errors:
     * - 429: Rate limited
     * - 529: API overloaded
     * - 500+: Server errors (except 529)
     *
     * @param throwable The error to check
     * @return true if the request should be retried
     */
    private boolean isRetryableError(Throwable throwable) {
        if (throwable instanceof WebClientResponseException e) {
            int status = e.getStatusCode().value();
            return status == 429 || status == 529 || (status >= 500 && status != 529);
        }
        return false;
    }

    /**
     * Checks if the Claude API is properly configured.
     *
     * @return true if API key is configured
     */
    public boolean isConfigured() {
        return config.isConfigured();
    }
}
