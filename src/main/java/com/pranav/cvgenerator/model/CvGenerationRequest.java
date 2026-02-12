/**
 * CvGenerationRequest.java
 *
 * Data Transfer Object (DTO) for CV generation API requests.
 * Captures the job description and optional metadata from the client.
 *
 * @author Pranav Ghorpade
 */
package com.pranav.cvgenerator.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * Request DTO for the CV generation endpoint.
 *
 * Expected JSON format:
 * {
 *   "jobDescription": "Full job description text...",
 *   "companyName": "Google" (optional)
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CvGenerationRequest {

    /**
     * The complete job description text.
     *
     * Validation:
     * - Required (cannot be blank)
     * - Minimum 50 characters (ensures meaningful content)
     * - Maximum 50,000 characters (prevents abuse)
     */
    @NotBlank(message = "Job description is required")
    @Size(min = 50, max = 50000,
          message = "Job description must be between 50 and 50,000 characters")
    private String jobDescription;

    /**
     * Optional company name for personalized PDF filename.
     * If not provided, will attempt to extract from job description.
     *
     * Used in filename: "PranavGhorpadeCv{CompanyName}.pdf"
     */
    @Size(max = 100, message = "Company name must be 100 characters or less")
    private String companyName;

    /**
     * Optional flag to skip cache and force regeneration.
     * Default is false (use cache when available).
     */
    @Builder.Default
    private boolean forceRegenerate = false;

    /**
     * CV experience level selection.
     *
     * Options:
     * - ENTRY_LEVEL: Fresh graduate CV with only Tesco experience (1 page)
     * - EXPERIENCED: Full CV with Red Fibre + SecurePoint + Tesco (2 pages)
     *
     * Default is EXPERIENCED to show full professional background.
     */
    @Builder.Default
    private ExperienceLevel experienceLevel = ExperienceLevel.EXPERIENCED;

    /**
     * Enum for CV experience level options.
     */
    public enum ExperienceLevel {
        /**
         * Entry-level/Fresh graduate CV.
         * Shows only: Education, Certifications, Projects, Skills, Tesco (current job).
         * Single page format suitable for graduate programs.
         */
        ENTRY_LEVEL,

        /**
         * Experienced professional CV.
         * Shows all: Red Fibre (1.5 years), SecurePoint (internship), Tesco,
         * Education, Certifications, Projects, Skills.
         * Two-page format showcasing full professional background.
         */
        EXPERIENCED
    }
}
