/**
 * CvGenerationRequest.java
 *
 * Data Transfer Object (DTO) for CV generation API requests.
 * Captures the job description and optional metadata from the client.
 *
 * @author Pranav Ghorpade
 * @version 1.0
 * @since 2025-01-XX
 *
 * Interview Note:
 * DTOs are used to separate the API contract from internal domain models.
 * This allows the API to evolve independently of the internal data structures.
 * Changes to the entity don't automatically break the API contract.
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
 *
 * Interview Note:
 * Using validation annotations ensures malformed requests are rejected
 * early with clear error messages, before any processing begins.
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
     *
     * Interview Note:
     * The @NotBlank annotation is stronger than @NotNull or @NotEmpty:
     * - @NotNull: Only checks for null
     * - @NotEmpty: Checks for null or empty string
     * - @NotBlank: Checks for null, empty, or whitespace-only
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
     *
     * Interview Note:
     * Cache-busting is useful during testing or when the user
     * wants a fresh generation despite having the same JD.
     */
    private boolean forceRegenerate = false;
}
