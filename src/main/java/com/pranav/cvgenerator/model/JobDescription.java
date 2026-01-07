/**
 * JobDescription.java
 *
 * Data model representing the job description input from the user.
 * Contains the raw JD text and optional metadata extracted during processing.
 *
 * @author Pranav Ghorpade
 * @version 1.0
 * @since 2025-01-XX
 *
 * Interview Note:
 * This class uses Bean Validation (JSR-380) annotations to ensure
 * the job description is not empty before processing. This validation
 * happens automatically in Spring when used with @Valid annotation.
 */
package com.pranav.cvgenerator.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * Model class for job description input.
 *
 * This class captures:
 * - The raw job description text pasted by the user
 * - Optional company name for PDF naming
 * - Optional job title for better tracking
 *
 * Interview Note:
 * Using @Builder from Lombok enables fluent object construction:
 * JobDescription.builder().content("...").companyName("Google").build();
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobDescription {

    /**
     * The raw job description text.
     *
     * Validation rules:
     * - Cannot be blank (null, empty, or whitespace only)
     * - Must be at least 50 characters (reasonable minimum for a JD)
     * - Maximum 50,000 characters (prevents abuse/memory issues)
     *
     * Interview Note:
     * These constraints are enforced by Spring's @Valid annotation
     * in the controller. Violations result in 400 Bad Request.
     */
    @NotBlank(message = "Job description cannot be empty")
    @Size(min = 50, max = 50000,
          message = "Job description must be between 50 and 50,000 characters")
    private String content;

    /**
     * Optional company name extracted from JD or provided by user.
     * Used for naming the generated PDF: "PranavGhorpadeCv{CompanyName}.pdf"
     */
    private String companyName;

    /**
     * Optional job title extracted from JD.
     * Helps in tracking and displaying recent generations.
     */
    private String jobTitle;

    /**
     * Hash of the content for caching purposes.
     * Allows quick lookup to avoid re-generating identical CVs.
     *
     * Interview Note:
     * Using content hash for caching is more reliable than comparing
     * full text strings, especially for large job descriptions.
     */
    private String contentHash;

    /**
     * Calculates and returns the MD5 hash of the job description content.
     * Used for cache lookup and duplicate detection.
     *
     * @return MD5 hash string of the content
     */
    public String calculateContentHash() {
        if (content == null || content.isBlank()) {
            return null;
        }
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            this.contentHash = sb.toString();
            return this.contentHash;
        } catch (java.security.NoSuchAlgorithmException e) {
            // MD5 is always available, this should never happen
            return String.valueOf(content.hashCode());
        }
    }
}
