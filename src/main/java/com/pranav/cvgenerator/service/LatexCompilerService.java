/**
 * LatexCompilerService.java
 *
 * Service for compiling LaTeX source code to PDF.
 * Uses external LaTeX API (latex.ytotech.com) for compilation.
 *
 * @author Pranav Ghorpade
 */
package com.pranav.cvgenerator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for LaTeX to PDF compilation.
 *
 * Uses YtoTech LaTeX API:
 * - Endpoint: https://latex.ytotech.com/builds/sync
 * - Method: POST
 * - Input: LaTeX source code + compiler options
 * - Output: PDF binary
 */
@Service
@Slf4j
public class LatexCompilerService {

    /**
     * Timeout for LaTeX compilation requests.
     * LaTeX can be slow for complex documents.
     */
    @Value("${latex.api.timeout:60000}")
    private int timeout;

    /**
     * WebClient configured for LaTeX API.
     */
    private final WebClient webClient;

    /**
     * JSON mapper for building requests.
     */
    private final ObjectMapper objectMapper;

    /**
     * Constructor with dependency injection.
     *
     * @param latexWebClient Configured WebClient for LaTeX API
     * @param objectMapper JSON mapper
     */
    public LatexCompilerService(
            @Qualifier("latexWebClient") WebClient latexWebClient,
            ObjectMapper objectMapper) {
        this.webClient = latexWebClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Compiles LaTeX source code to PDF.
     *
     * Process:
     * 1. Build the API request with LaTeX source
     * 2. Send to YtoTech API
     * 3. Receive PDF binary
     * 4. Return as byte array
     *
     * @param latexSource Complete LaTeX document source
     * @return PDF content as byte array
     * @throws LatexCompilationException if compilation fails
     */
    public byte[] compileToPdf(String latexSource) {
        log.info("Starting LaTeX compilation...");
        long startTime = System.currentTimeMillis();

        try {
            // Build the request body
            Map<String, Object> requestBody = buildCompilationRequest(latexSource);

            // Make the API call
            byte[] pdfBytes = webClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block(Duration.ofMillis(timeout));

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("LaTeX compilation completed in {}ms, PDF size: {} bytes",
                    elapsed, pdfBytes != null ? pdfBytes.length : 0);

            if (pdfBytes == null || pdfBytes.length == 0) {
                throw new LatexCompilationException("Empty PDF returned from compiler");
            }

            return pdfBytes;

        } catch (WebClientResponseException e) {
            log.error("LaTeX API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new LatexCompilationException("LaTeX compilation failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to compile LaTeX", e);
            throw new LatexCompilationException("Failed to compile PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Builds the request body for YtoTech LaTeX API.
     *
     * Request format:
     * {
     *   "compiler": "pdflatex",
     *   "resources": [
     *     {
     *       "main": true,
     *       "content": "\\documentclass..."
     *     }
     *   ]
     * }
     *
     * @param latexSource LaTeX source code
     * @return Request body map
     */
    private Map<String, Object> buildCompilationRequest(String latexSource) {
        Map<String, Object> request = new HashMap<>();

        // Specify the compiler
        request.put("compiler", "pdflatex");

        // Add the LaTeX source as the main resource
        Map<String, Object> mainResource = new HashMap<>();
        mainResource.put("main", true);
        mainResource.put("content", latexSource);

        request.put("resources", List.of(mainResource));

        return request;
    }

    /**
     * Validates that the LaTeX source is compilable.
     *
     * Basic validation checks:
     * - Contains \documentclass
     * - Contains \begin{document}
     * - Contains \end{document}
     *
     * @param latexSource LaTeX source to validate
     * @return true if source passes basic validation
     */
    public boolean validateLatexSource(String latexSource) {
        if (latexSource == null || latexSource.isBlank()) {
            return false;
        }

        return latexSource.contains("\\documentclass") &&
               latexSource.contains("\\begin{document}") &&
               latexSource.contains("\\end{document}");
    }

    /**
     * Custom exception for LaTeX compilation errors.
     */
    public static class LatexCompilationException extends RuntimeException {
        public LatexCompilationException(String message) {
            super(message);
        }

        public LatexCompilationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
