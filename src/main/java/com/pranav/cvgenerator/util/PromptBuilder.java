/**
 * PromptBuilder.java
 *
 * Utility class for building prompts sent to Claude API.
 * Combines system prompt, candidate data, and job description.
 *
 * @author Pranav Ghorpade
 * @version 1.0
 * @since 2025-01-XX
 *
 * Interview Note:
 * Prompt engineering is crucial for getting good results from LLMs.
 * This class encapsulates the prompt construction logic, making it
 * easy to modify and test different prompt strategies.
 */
package com.pranav.cvgenerator.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pranav.cvgenerator.model.CandidateProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Builder for Claude API prompts.
 *
 * Responsibilities:
 * 1. Load system prompt from resources
 * 2. Format candidate data as JSON
 * 3. Combine all parts into structured user message
 * 4. Handle special characters and escaping
 *
 * Interview Note:
 * @Component makes this class a Spring-managed bean, allowing
 * it to be autowired into other components.
 */
@Component
@Slf4j
public class PromptBuilder {

    /**
     * Path to the CV_GEN system prompt file.
     */
    private static final String SYSTEM_PROMPT_FILE = "cv-gen-system-prompt.txt";

    /**
     * Cached system prompt loaded from file.
     */
    private String cachedSystemPrompt;

    /**
     * JSON mapper for serializing candidate data.
     */
    private final ObjectMapper objectMapper;

    /**
     * Constructor with dependency injection.
     *
     * @param objectMapper Jackson ObjectMapper
     */
    public PromptBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Initializes the prompt builder by loading the system prompt.
     *
     * Called automatically after bean construction.
     */
    @PostConstruct
    public void init() {
        try {
            loadSystemPromptFromFile();
            log.info("System prompt loaded successfully ({} characters)",
                    cachedSystemPrompt.length());
        } catch (IOException e) {
            log.error("Failed to load system prompt", e);
            throw new RuntimeException("Failed to load system prompt: " + e.getMessage(), e);
        }
    }

    /**
     * Loads the system prompt from the resources file.
     *
     * @throws IOException if file cannot be read
     */
    private void loadSystemPromptFromFile() throws IOException {
        ClassPathResource resource = new ClassPathResource(SYSTEM_PROMPT_FILE);
        cachedSystemPrompt = new String(
                resource.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );
    }

    /**
     * Returns the cached system prompt.
     *
     * The system prompt defines CV_GEN's behavior, including:
     * - Recruiter expectation models
     * - Keyword tiering logic
     * - Output format requirements
     * - LaTeX template structure
     * - Writing style rules
     *
     * @return The CV_GEN system prompt
     */
    public String loadSystemPrompt() {
        if (cachedSystemPrompt == null || cachedSystemPrompt.isBlank()) {
            throw new IllegalStateException("System prompt not loaded");
        }
        return cachedSystemPrompt;
    }

    /**
     * Builds the complete user message for Claude.
     *
     * The message is structured in sections:
     * 1. CANDIDATE DATA - Full profile as JSON
     * 2. JOB DESCRIPTION - User's pasted JD
     * 3. INSTRUCTIONS - What to generate
     *
     * Interview Note:
     * Clear section markers help the LLM understand the different
     * parts of the input. JSON format for candidate data ensures
     * consistent parsing.
     *
     * @param candidate The candidate profile
     * @param jobDescription The job description text
     * @return Formatted user message
     */
    public String buildUserMessage(CandidateProfile candidate, String jobDescription) {
        StringBuilder message = new StringBuilder();

        // Section 1: Candidate Data
        message.append("---CANDIDATE DATA---\n");
        message.append(formatCandidateData(candidate));
        message.append("\n\n");

        // Section 2: Job Description
        message.append("---JOB DESCRIPTION---\n");
        message.append(jobDescription.trim());
        message.append("\n\n");

        // Section 3: Instructions
        message.append("---INSTRUCTIONS---\n");
        message.append(buildInstructions());

        return message.toString();
    }

    /**
     * Formats the candidate profile as a JSON string.
     *
     * @param candidate The candidate profile
     * @return JSON representation of the profile
     */
    private String formatCandidateData(CandidateProfile candidate) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(candidate);
        } catch (Exception e) {
            log.error("Failed to serialize candidate data", e);
            throw new RuntimeException("Failed to format candidate data", e);
        }
    }

    /**
     * Builds the instruction section for the prompt.
     *
     * These instructions tell Claude specifically what to generate
     * and in what format.
     *
     * @return Instruction text
     */
    private String buildInstructions() {
        return """
            Generate a tailored CV for the candidate based on the job description above.

            REQUIREMENTS:
            1. Use the EXACT LaTeX template structure from the system prompt
            2. PRESERVE ALL candidate data - do not omit experiences, projects, or certifications
            3. Tailor the Professional Profile/Summary to match the JD keywords
            4. Reorder sections based on the detected recruiter model's priorities
            5. Include ALL bullet points from experience, adjusting language to match JD
            6. Select the most relevant 4-5 projects based on JD requirements
            7. Properly escape LaTeX special characters: #, $, %, &, _, {, }, ~, ^, \\
            8. Maintain the charter font and twocolentry/onecolentry formatting

            OUTPUT FORMAT:
            Return a valid JSON object with the structure defined in the system prompt.
            The latex_cv field must contain complete, compilable LaTeX code.

            IMPORTANT:
            - The CV must fit on ONE PAGE
            - Use the candidate's ACTUAL data - do not invent information
            - Every technical claim must be supported by the candidate's experience or projects
            - If including the Tesco retail role, only if soft skills are relevant to JD
            """;
    }

    /**
     * Reloads the system prompt from file.
     *
     * Useful for development when modifying the prompt.
     *
     * @throws IOException if file cannot be read
     */
    public void reloadSystemPrompt() throws IOException {
        log.info("Reloading system prompt...");
        loadSystemPromptFromFile();
        log.info("System prompt reloaded ({} characters)", cachedSystemPrompt.length());
    }
}
