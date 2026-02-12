/**
 * CvGenerationResponse.java
 *
 * Data Transfer Object (DTO) for CV generation API responses.
 * Contains the generation results including URLs for downloading
 * the generated CV and analysis data.
 *
 * @author Pranav Ghorpade
 */
package com.pranav.cvgenerator.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for CV generation results.
 *
 * Example response:
 * {
 *   "id": "550e8400-e29b-41d4-a716-446655440000",
 *   "status": "COMPLETED",
 *   "matchScore": { "keywordCoverage": 92, "recruiterFit": 87 },
 *   "pdfUrl": "/api/download/550e.../pdf",
 *   "texUrl": "/api/download/550e.../tex",
 *   "coachBrief": { ... }
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CvGenerationResponse {

    /**
     * Unique identifier for this generation.
     * Used to retrieve results, check status, and download files.
     */
    private String id;

    /**
     * Current status of the generation.
     * Values: "PROCESSING", "COMPLETED", "FAILED"
     */
    private String status;

    /**
     * Progress percentage (0-100) during processing.
     * Only relevant when status is "PROCESSING".
     */
    private Integer progress;

    /**
     * Current processing step description.
     * Examples: "Analyzing job description", "Generating LaTeX", "Compiling PDF"
     */
    private String currentStep;

    /**
     * Detected recruiter domain/role type.
     * Values: "backend", "devops", "security", "fullstack", "other"
     */
    private String recruiterDomain;

    /**
     * Company name extracted from JD or provided by user.
     */
    private String companyName;

    /**
     * Job title extracted from JD.
     */
    private String jobTitle;

    /**
     * Match scores from Claude's analysis.
     */
    private MatchScore matchScore;

    /**
     * Categorized keywords from the job description.
     */
    private DetectedKeywords detectedKeywords;

    /**
     * Keyword tier classification based on candidate's profile.
     */
    private KeywordTiers keywordTiers;

    /**
     * URL to download the generated PDF.
     * Format: "/api/download/{id}/pdf"
     */
    private String pdfUrl;

    /**
     * URL to download the LaTeX source.
     * Format: "/api/download/{id}/tex"
     */
    private String texUrl;

    /**
     * Coach brief containing skill gaps and learning roadmap.
     */
    private CoachBrief coachBrief;

    /**
     * Timestamp when generation was requested.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when generation completed.
     */
    private LocalDateTime completedAt;

    /**
     * Error message if generation failed.
     * Only present when status is "FAILED".
     */
    private String errorMessage;

    // ==================== NESTED CLASSES ====================

    /**
     * Match score metrics from CV analysis.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MatchScore {
        /**
         * Percentage of JD keywords covered in the CV (0-100).
         */
        private Integer keywordCoverage;

        /**
         * Overall recruiter fit score (0-100).
         */
        private Integer recruiterFit;
    }

    /**
     * Keywords detected and categorized from the job description.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DetectedKeywords {
        /**
         * Must-have/required keywords from the JD.
         */
        private List<String> mustHave;

        /**
         * Nice-to-have/preferred keywords from the JD.
         */
        private List<String> niceToHave;

        /**
         * Soft skills mentioned in the JD.
         */
        private List<String> softSkills;
    }

    /**
     * Keywords classified by candidate's proficiency level.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KeywordTiers {
        /**
         * Keywords where candidate has proven proficiency.
         */
        private List<String> proficient;

        /**
         * Keywords where candidate has exposure/basic knowledge.
         */
        private List<String> exposure;

        /**
         * Keywords that are new to the candidate (learning opportunities).
         */
        private List<String> awareness;
    }

    /**
     * Coach brief for skill development and interview preparation.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CoachBrief {
        /**
         * Identified skill gaps between candidate and JD requirements.
         */
        private List<String> skillGaps;

        /**
         * Structured learning roadmap with time-based goals.
         */
        private LearningRoadmap learningRoadmap;

        /**
         * Potential interview questions based on JD.
         */
        private List<String> interviewQuestions;
    }

    /**
     * Time-based learning roadmap for skill development.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LearningRoadmap {
        /**
         * Learning goals for the first 7 days.
         */
        private List<String> sevenDays;

        /**
         * Learning goals for days 8-14.
         */
        private List<String> fourteenDays;

        /**
         * Learning goals for days 15-21.
         */
        private List<String> twentyOneDays;
    }

    /**
     * Factory method to create a "processing" response.
     *
     * @param id The generation ID
     * @return Response indicating processing status
     */
    public static CvGenerationResponse processing(String id) {
        return CvGenerationResponse.builder()
                .id(id)
                .status("PROCESSING")
                .progress(0)
                .currentStep("Starting CV generation...")
                .build();
    }

    /**
     * Factory method to create a "failed" response.
     *
     * @param id The generation ID
     * @param errorMessage Description of the error
     * @return Response indicating failure
     */
    public static CvGenerationResponse failed(String id, String errorMessage) {
        return CvGenerationResponse.builder()
                .id(id)
                .status("FAILED")
                .errorMessage(errorMessage)
                .build();
    }
}
