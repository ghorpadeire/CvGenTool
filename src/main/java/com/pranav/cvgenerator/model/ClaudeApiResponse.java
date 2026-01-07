/**
 * ClaudeApiResponse.java
 *
 * Data model representing the parsed response from Claude API.
 * Maps the JSON structure returned by CV_GEN system prompt.
 *
 * @author Pranav Ghorpade
 * @version 1.0
 * @since 2025-01-XX
 *
 * Interview Note:
 * This class maps the specific JSON structure defined in the CV_GEN system prompt.
 * Using Jackson annotations for flexible JSON parsing with snake_case to camelCase conversion.
 */
package com.pranav.cvgenerator.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Response model for Claude API CV generation.
 *
 * Expected JSON structure from Claude:
 * {
 *   "status": "ok",
 *   "recruiter_model": { ... },
 *   "keyword_tiers": { ... },
 *   "match_score": { ... },
 *   "latex_cv": "...",
 *   "coach_brief": { ... }
 * }
 *
 * Interview Note:
 * @JsonIgnoreProperties(ignoreUnknown = true) ensures the parser doesn't fail
 * if Claude returns additional fields not mapped in this class. This makes
 * the integration more resilient to API changes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClaudeApiResponse {

    /**
     * Status of the generation.
     * Expected value: "ok" for successful generation.
     */
    private String status;

    /**
     * Recruiter model analysis containing domain and keywords.
     */
    @JsonProperty("recruiter_model")
    private RecruiterModel recruiterModel;

    /**
     * Classification of keywords by candidate proficiency.
     */
    @JsonProperty("keyword_tiers")
    private KeywordTiers keywordTiers;

    /**
     * Match scores for keyword coverage and recruiter fit.
     */
    @JsonProperty("match_score")
    private MatchScore matchScore;

    /**
     * Complete LaTeX source code for the CV.
     * This is the main output that gets compiled to PDF.
     */
    @JsonProperty("latex_cv")
    private String latexCv;

    /**
     * Coach brief with skill gaps and learning roadmap.
     */
    @JsonProperty("coach_brief")
    private CoachBrief coachBrief;

    // ==================== NESTED CLASSES ====================

    /**
     * Recruiter model analysis.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RecruiterModel {
        /**
         * Detected role domain.
         * Values: "backend", "devops", "security", "fullstack", "other"
         */
        private String domain;

        /**
         * Keywords detected from the job description.
         */
        @JsonProperty("detected_keywords")
        private DetectedKeywords detectedKeywords;
    }

    /**
     * Keywords detected from the job description.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DetectedKeywords {
        /**
         * Required/must-have skills from JD.
         */
        @JsonProperty("must_have")
        private List<String> mustHave;

        /**
         * Nice-to-have/preferred skills from JD.
         */
        @JsonProperty("nice_to_have")
        private List<String> niceToHave;

        /**
         * Soft skills mentioned in JD.
         */
        @JsonProperty("soft_skills")
        private List<String> softSkills;
    }

    /**
     * Keyword classification by candidate proficiency.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KeywordTiers {
        /**
         * Keywords with proven proficiency.
         */
        private List<String> proficient;

        /**
         * Keywords with exposure/basic knowledge.
         */
        private List<String> exposure;

        /**
         * New keywords (learning opportunities).
         */
        private List<String> awareness;
    }

    /**
     * Match score metrics.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MatchScore {
        /**
         * Percentage of JD keywords covered (0-100).
         */
        @JsonProperty("keyword_coverage_pct")
        private Integer keywordCoveragePct;

        /**
         * Recruiter fit score (0-100).
         */
        @JsonProperty("recruiter_fit_pct")
        private Integer recruiterFitPct;
    }

    /**
     * Coach brief for skill development.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CoachBrief {
        /**
         * Identified skill gaps.
         */
        @JsonProperty("skill_gaps")
        private List<String> skillGaps;

        /**
         * Learning roadmap with time-based goals.
         */
        @JsonProperty("learning_roadmap")
        private LearningRoadmap learningRoadmap;

        /**
         * Potential interview questions.
         */
        @JsonProperty("interview_questions")
        private List<String> interviewQuestions;
    }

    /**
     * Time-based learning roadmap.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LearningRoadmap {
        /**
         * Goals for days 1-7.
         */
        @JsonProperty("7_days")
        private List<String> sevenDays;

        /**
         * Goals for days 8-14.
         */
        @JsonProperty("14_days")
        private List<String> fourteenDays;

        /**
         * Goals for days 15-21.
         */
        @JsonProperty("21_days")
        private List<String> twentyOneDays;
    }

    /**
     * Checks if the response indicates successful generation.
     *
     * @return true if status is "ok" and latex_cv is present
     */
    public boolean isSuccess() {
        return "ok".equalsIgnoreCase(status) && latexCv != null && !latexCv.isBlank();
    }
}
