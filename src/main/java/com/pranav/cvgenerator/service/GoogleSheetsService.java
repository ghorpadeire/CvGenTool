/**
 * GoogleSheetsService.java
 *
 * Service for storing CV generation history in Google Sheets.
 * Stores: Date, Company, Job Description, PDF Link, Coach Brief
 *
 * @author Pranav Ghorpade
 * @version 1.0
 */
package com.pranav.cvgenerator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service to log CV generations to Google Sheets via Apps Script Web App.
 *
 * This uses a simple Google Apps Script webhook approach - no OAuth complexity!
 * The Apps Script handles authentication and writes to the sheet.
 */
@Service
@Slf4j
public class GoogleSheetsService {

    @Value("${google.sheets.webhook.url:}")
    private String webhookUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Logs a CV generation to Google Sheets.
     *
     * @param companyName The target company name
     * @param jobDescription The job description (truncated for storage)
     * @param pdfUrl URL to the generated PDF (if stored in Drive)
     * @param coachBrief The coaching/interview prep brief
     * @param matchScore The keyword match percentage
     */
    public void logGeneration(String companyName, String jobDescription,
                              String pdfUrl, String coachBrief, int matchScore) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            log.debug("Google Sheets webhook not configured, skipping log");
            return;
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("date", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            payload.put("company", companyName != null ? companyName : "Unknown");
            payload.put("jobDescription", truncate(jobDescription, 500));
            payload.put("pdfUrl", pdfUrl != null ? pdfUrl : "");
            payload.put("coachBrief", truncate(coachBrief, 1000));
            payload.put("matchScore", matchScore);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            log.info("Logging CV generation to Google Sheets for company: {}", companyName);
            ResponseEntity<String> response = restTemplate.postForEntity(webhookUrl, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully logged to Google Sheets");
            } else {
                log.warn("Failed to log to Google Sheets: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error logging to Google Sheets: {}", e.getMessage());
            // Don't throw - logging failure shouldn't break CV generation
        }
    }

    /**
     * Truncates text to specified length with ellipsis.
     */
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}
