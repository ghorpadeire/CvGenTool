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
     * Builds the complete user message for Claude (defaults to EXPERIENCED level).
     *
     * @param candidate The candidate profile
     * @param jobDescription The job description text
     * @return Formatted user message
     */
    public String buildUserMessage(CandidateProfile candidate, String jobDescription) {
        return buildUserMessage(candidate, jobDescription, "EXPERIENCED");
    }

    /**
     * Builds the complete user message for Claude with experience level selection.
     *
     * The message is structured in sections:
     * 1. CANDIDATE DATA - Full profile as JSON
     * 2. JOB DESCRIPTION - User's pasted JD
     * 3. EXPERIENCE LEVEL - ENTRY_LEVEL or EXPERIENCED
     * 4. INSTRUCTIONS - What to generate (varies by level)
     *
     * Interview Note:
     * Clear section markers help the LLM understand the different
     * parts of the input. JSON format for candidate data ensures
     * consistent parsing.
     *
     * @param candidate The candidate profile
     * @param jobDescription The job description text
     * @param experienceLevel ENTRY_LEVEL or EXPERIENCED
     * @return Formatted user message
     */
    public String buildUserMessage(CandidateProfile candidate, String jobDescription, String experienceLevel) {
        StringBuilder message = new StringBuilder();

        // Section 1: Candidate Data
        message.append("---CANDIDATE DATA---\n");
        message.append(formatCandidateData(candidate));
        message.append("\n\n");

        // Section 2: Job Description
        message.append("---JOB DESCRIPTION---\n");
        message.append(jobDescription.trim());
        message.append("\n\n");

        // Section 3: Experience Level
        message.append("---EXPERIENCE LEVEL---\n");
        message.append(experienceLevel != null ? experienceLevel : "EXPERIENCED");
        message.append("\n\n");

        // Section 4: Instructions based on experience level
        message.append("---INSTRUCTIONS---\n");
        message.append(buildInstructions(experienceLevel));

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
     * Builds the instruction section for the prompt (defaults to EXPERIENCED).
     *
     * @return Instruction text
     */
    private String buildInstructions() {
        return buildInstructions("EXPERIENCED");
    }

    /**
     * Builds the instruction section for the prompt based on experience level.
     *
     * These instructions tell Claude specifically what to generate
     * and in what format. Different instructions for entry vs experienced.
     *
     * @param experienceLevel ENTRY_LEVEL or EXPERIENCED
     * @return Instruction text
     */
    private String buildInstructions(String experienceLevel) {
        if ("ENTRY_LEVEL".equals(experienceLevel)) {
            return buildEntryLevelInstructions();
        } else {
            return buildExperiencedInstructions();
        }
    }

    /**
     * Instructions for ENTRY_LEVEL CV generation.
     * Fresh graduate style - 1 page, no tech experience shown.
     */
    private String buildEntryLevelInstructions() {
        return """
            Generate a FRESH GRADUATE / ENTRY-LEVEL CV for the candidate.

            CRITICAL - ENTRY LEVEL RULES:
            1. DO NOT include Red Fibre backend developer experience
            2. DO NOT include SecurePoint cybersecurity internship
            3. ONLY include current Tesco employment to show work ethic
            4. Position as a FRESH GRADUATE with strong academic background
            5. Emphasize: Education (MSc + BTech), Certifications (CEH Master), Projects, Skills
            6. The CV MUST be 1 PAGE ONLY

            CV STRUCTURE FOR ENTRY LEVEL:
            1. Header (Name, Contact, LinkedIn, GitHub, Medium)
            2. Professional Profile (Fresh graduate with MSc Cybersecurity, CEH Master, strong projects)
            3. Education (MSc Cybersecurity - NCI Dublin, BTech Computer Engineering)
            4. Certifications (CEH Master v12 is the highlight!)
            5. Technical Skills
            6. Open Source Contribution (TheAlgorithms/Java - 59K+ stars)
            7. Key Projects (5-6 most relevant projects)
            8. DevOps Program (8-week intensive)
            9. Current Employment (Tesco - shows work ethic, reliability)
            10. Core Strengths

            REQUIREMENTS:
            1. Use the EXACT LaTeX template structure from the system prompt
            2. Tailor the Professional Profile to match JD keywords as a FRESH GRADUATE
            3. Select 5-6 most relevant projects based on JD requirements
            4. Properly escape LaTeX special characters: #, $, %, &, _, {, }, ~, ^, \\
            5. Maintain the charter font and twocolentry/onecolentry formatting

            OUTPUT FORMAT:
            Return a valid JSON object with the structure defined in the system prompt.
            The latex_cv field must contain complete, compilable LaTeX code.

            IMPORTANT:
            - The CV MUST fit on ONE PAGE
            - Use the candidate's ACTUAL data - do not invent information
            - Position as fresh MSc graduate seeking entry-level opportunity
            - Emphasize: Education, CEH Master certification, Projects, Open Source contribution
            - Include Tesco to show work ethic and communication skills
            - Always mention: Stamp 1G visa - authorized to work in Ireland
            """;
    }

    /**
     * Instructions for EXPERIENCED CV generation.
     * Professional with 1.5+ years experience - 2 pages.
     */
    private String buildExperiencedInstructions() {
        return """
            Generate an EXPERIENCED PROFESSIONAL CV for the candidate.

            CRITICAL - EXPERIENCED LEVEL RULES:
            1. MUST include Red Fibre Backend Developer experience (Apr 2022 - Aug 2023) - 1.5 years
            2. MUST include SecurePoint Solutions Cybersecurity Internship (Jan 2022 - Mar 2022)
            3. Include Tesco current employment to show adaptability
            4. Position as JUNIOR/MID-LEVEL DEVELOPER with 1.5+ years tech experience
            5. The CV should be 2 PAGES to showcase full professional background

            CV STRUCTURE FOR EXPERIENCED:
            1. Header (Name, Contact, LinkedIn, GitHub, Medium)
            2. Professional Profile (Backend Developer with 1.5+ years at Red Fibre, SecurePoint intern)
            3. Professional Experience:
               - Red Fibre Backend Developer (DETAILED - all 8 bullet points with metrics)
               - SecurePoint Cybersecurity Intern (all 6 bullet points)
               - Tesco Retail Associate (brief - shows adaptability)
            4. Education (MSc Cybersecurity - NCI Dublin, BTech Computer Engineering)
            5. Certifications (CEH Master v12 is the highlight!)
            6. Technical Skills (proficient from work experience)
            7. Open Source Contribution (TheAlgorithms/Java - 59K+ stars)
            8. Key Projects (6-7 most relevant projects)
            9. DevOps Program (8-week intensive)
            10. Core Strengths

            REQUIREMENTS:
            1. Use the EXACT LaTeX template structure from the system prompt
            2. PRESERVE ALL experience data - include ALL bullet points from Red Fibre and SecurePoint
            3. Tailor the Professional Profile to match JD keywords as EXPERIENCED developer
            4. Include ALL 7 projects mentioned in candidate data
            5. Properly escape LaTeX special characters: #, $, %, &, _, {, }, ~, ^, \\
            6. Maintain the charter font and twocolentry/onecolentry formatting

            OUTPUT FORMAT:
            Return a valid JSON object with the structure defined in the system prompt.
            The latex_cv field must contain complete, compilable LaTeX code.

            IMPORTANT:
            - The CV should be 2 PAGES to include all experience and projects
            - Use the candidate's ACTUAL data - do not invent information
            - Position as experienced developer (1.5+ years) seeking mid-level roles
            - Include specific metrics: 10,000+ daily requests, 99.9% uptime, 85% code coverage, etc.
            - Emphasize: Professional Experience, Certifications, Projects
            - Always mention: Stamp 1G visa - authorized to work in Ireland
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
