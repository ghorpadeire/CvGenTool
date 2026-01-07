/**
 * JsonParser.java
 *
 * Utility class for parsing JSON responses from Claude API.
 * Handles various edge cases and response formats.
 *
 * @author Pranav Ghorpade
 * @version 1.0
 * @since 2025-01-XX
 *
 * Interview Note:
 * This utility provides robust JSON parsing with error handling.
 * Claude responses can sometimes have slight formatting variations,
 * so we need flexible parsing logic.
 */
package com.pranav.cvgenerator.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pranav.cvgenerator.model.ClaudeApiResponse;
import com.pranav.cvgenerator.model.CvGenerationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for JSON parsing operations.
 *
 * Provides methods for:
 * - Parsing Claude API responses
 * - Converting between response formats
 * - Extracting specific fields from JSON
 * - Handling malformed JSON gracefully
 *
 * Interview Note:
 * Centralizing JSON parsing logic makes it easier to handle
 * edge cases and maintain consistent error handling.
 */
@Component
@Slf4j
public class JsonParser {

    /**
     * Jackson ObjectMapper for JSON operations.
     */
    private final ObjectMapper objectMapper;

    /**
     * Constructor with dependency injection.
     *
     * @param objectMapper Jackson ObjectMapper
     */
    public JsonParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Parses a JSON string into a ClaudeApiResponse object.
     *
     * Handles:
     * - Direct JSON objects
     * - JSON wrapped in markdown code blocks
     * - Partial/incomplete JSON
     *
     * @param json The JSON string to parse
     * @return Parsed ClaudeApiResponse
     * @throws JsonParseException if parsing fails
     */
    public ClaudeApiResponse parseClaudeResponse(String json) {
        try {
            // Clean the JSON (remove code blocks, trim, etc.)
            String cleanedJson = cleanJsonString(json);

            // Parse to ClaudeApiResponse
            return objectMapper.readValue(cleanedJson, ClaudeApiResponse.class);

        } catch (JsonProcessingException e) {
            log.error("Failed to parse Claude response: {}", json, e);
            throw new JsonParseException("Failed to parse Claude response", e);
        }
    }

    /**
     * Converts a ClaudeApiResponse to a CvGenerationResponse DTO.
     *
     * Maps the internal Claude response format to the external API format.
     *
     * @param claudeResponse The Claude API response
     * @param cvId The CV generation ID
     * @param companyName The company name for URL generation
     * @return CvGenerationResponse for API output
     */
    public CvGenerationResponse toGenerationResponse(
            ClaudeApiResponse claudeResponse,
            String cvId,
            String companyName) {

        CvGenerationResponse.CvGenerationResponseBuilder builder =
                CvGenerationResponse.builder()
                        .id(cvId)
                        .status("COMPLETED");

        // Map recruiter model
        if (claudeResponse.getRecruiterModel() != null) {
            builder.recruiterDomain(claudeResponse.getRecruiterModel().getDomain());

            if (claudeResponse.getRecruiterModel().getDetectedKeywords() != null) {
                ClaudeApiResponse.DetectedKeywords dk =
                        claudeResponse.getRecruiterModel().getDetectedKeywords();

                builder.detectedKeywords(CvGenerationResponse.DetectedKeywords.builder()
                        .mustHave(dk.getMustHave())
                        .niceToHave(dk.getNiceToHave())
                        .softSkills(dk.getSoftSkills())
                        .build());
            }
        }

        // Map match scores
        if (claudeResponse.getMatchScore() != null) {
            builder.matchScore(CvGenerationResponse.MatchScore.builder()
                    .keywordCoverage(claudeResponse.getMatchScore().getKeywordCoveragePct())
                    .recruiterFit(claudeResponse.getMatchScore().getRecruiterFitPct())
                    .build());
        }

        // Map keyword tiers
        if (claudeResponse.getKeywordTiers() != null) {
            ClaudeApiResponse.KeywordTiers kt = claudeResponse.getKeywordTiers();
            builder.keywordTiers(CvGenerationResponse.KeywordTiers.builder()
                    .proficient(kt.getProficient())
                    .exposure(kt.getExposure())
                    .awareness(kt.getAwareness())
                    .build());
        }

        // Map coach brief
        if (claudeResponse.getCoachBrief() != null) {
            ClaudeApiResponse.CoachBrief cb = claudeResponse.getCoachBrief();

            CvGenerationResponse.LearningRoadmap roadmap = null;
            if (cb.getLearningRoadmap() != null) {
                roadmap = CvGenerationResponse.LearningRoadmap.builder()
                        .sevenDays(cb.getLearningRoadmap().getSevenDays())
                        .fourteenDays(cb.getLearningRoadmap().getFourteenDays())
                        .twentyOneDays(cb.getLearningRoadmap().getTwentyOneDays())
                        .build();
            }

            builder.coachBrief(CvGenerationResponse.CoachBrief.builder()
                    .skillGaps(cb.getSkillGaps())
                    .learningRoadmap(roadmap)
                    .interviewQuestions(cb.getInterviewQuestions())
                    .build());
        }

        // Set download URLs
        builder.pdfUrl("/api/download/" + cvId + "/pdf");
        builder.texUrl("/api/download/" + cvId + "/tex");
        builder.companyName(companyName);

        return builder.build();
    }

    /**
     * Cleans a JSON string by removing code blocks and whitespace.
     *
     * @param json The raw JSON string
     * @return Cleaned JSON string
     */
    public String cleanJsonString(String json) {
        if (json == null) {
            return null;
        }

        String cleaned = json.trim();

        // Remove markdown code block markers
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }

        // Find JSON object boundaries
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');

        if (start != -1 && end != -1 && end > start) {
            cleaned = cleaned.substring(start, end + 1);
        }

        return cleaned.trim();
    }

    /**
     * Extracts a string field from a JSON node.
     *
     * @param node The JSON node
     * @param fieldName The field to extract
     * @return Field value or null if not present
     */
    public String extractString(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName)) {
            return null;
        }
        return node.get(fieldName).asText(null);
    }

    /**
     * Extracts an integer field from a JSON node.
     *
     * @param node The JSON node
     * @param fieldName The field to extract
     * @param defaultValue Default value if field not present
     * @return Field value or default
     */
    public int extractInt(JsonNode node, String fieldName, int defaultValue) {
        if (node == null || !node.has(fieldName)) {
            return defaultValue;
        }
        return node.get(fieldName).asInt(defaultValue);
    }

    /**
     * Extracts a string list from a JSON array node.
     *
     * @param node The JSON node containing an array
     * @param fieldName The field containing the array
     * @return List of strings or empty list
     */
    public List<String> extractStringList(JsonNode node, String fieldName) {
        List<String> result = new ArrayList<>();

        if (node == null || !node.has(fieldName)) {
            return result;
        }

        JsonNode arrayNode = node.get(fieldName);
        if (arrayNode.isArray()) {
            for (JsonNode item : arrayNode) {
                if (item.isTextual()) {
                    result.add(item.asText());
                }
            }
        }

        return result;
    }

    /**
     * Converts an object to JSON string.
     *
     * @param object The object to serialize
     * @return JSON string representation
     */
    public String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object to JSON", e);
            throw new JsonParseException("Failed to serialize to JSON", e);
        }
    }

    /**
     * Converts an object to pretty-printed JSON string.
     *
     * @param object The object to serialize
     * @return Pretty-printed JSON string
     */
    public String toPrettyJson(Object object) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object to JSON", e);
            throw new JsonParseException("Failed to serialize to JSON", e);
        }
    }

    /**
     * Custom exception for JSON parsing errors.
     */
    public static class JsonParseException extends RuntimeException {
        public JsonParseException(String message) {
            super(message);
        }

        public JsonParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
