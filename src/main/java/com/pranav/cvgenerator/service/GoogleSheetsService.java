/**
 * GoogleSheetsService.java
 *
 * Service for storing CV generation history in Google Sheets.
 * Also uploads PDFs to Google Drive and stores the link.
 *
 * @author Pranav Ghorpade
 * @version 1.1
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
 * Features:
 * - Uploads PDF to Google Drive (via Apps Script)
 * - Logs generation details to Google Sheets
 * - Stores shareable PDF link in the sheet
 */
@Service
@Slf4j
public class GoogleSheetsService {

    @Value("${google.sheets.webhook.url:}")
    private String webhookUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Logs a CV generation to Google Sheets with PDF upload.
     *
     * @param companyName The target company name
     * @param jobDescription The job description (truncated for storage)
     * @param pdfBytes The generated PDF file bytes
     * @param pdfFilename The filename for the PDF
     * @param coachBrief The coaching/interview prep brief (JSON)
     * @param matchScore The keyword match percentage
     */
    public void logGenerationWithPdf(String companyName, String jobDescription,
                                      byte[] pdfBytes, String pdfFilename,
                                      String coachBrief, int matchScore) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            log.debug("Google Sheets webhook not configured, skipping log");
            return;
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("date", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            payload.put("company", companyName != null ? companyName : "Unknown");
            payload.put("jobDescription", truncate(jobDescription, 500));
            payload.put("coachBrief", coachBrief != null ? coachBrief : "");
            payload.put("matchScore", matchScore);

            // Add PDF as base64 for Google Drive upload
            if (pdfBytes != null && pdfBytes.length > 0) {
                String pdfBase64 = Base64.getEncoder().encodeToString(pdfBytes);
                payload.put("pdfBase64", pdfBase64);
                payload.put("pdfFilename", pdfFilename != null ? pdfFilename : "CV.pdf");
                log.info("Including PDF ({} bytes) for upload to Google Drive", pdfBytes.length);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            log.info("Logging CV generation to Google Sheets for company: {}", companyName);
            ResponseEntity<String> response = restTemplate.postForEntity(webhookUrl, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully logged to Google Sheets with PDF");
            } else {
                log.warn("Failed to log to Google Sheets: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error logging to Google Sheets: {}", e.getMessage());
            // Don't throw - logging failure shouldn't break CV generation
        }
    }

    /**
     * Legacy method for logging without PDF.
     * Kept for backward compatibility.
     */
    public void logGeneration(String companyName, String jobDescription,
                              String pdfUrl, String coachBrief, int matchScore) {
        logGenerationWithPdf(companyName, jobDescription, null, null, coachBrief, matchScore);
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
