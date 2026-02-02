/**
 * CvGenerationService.java
 *
 * Main orchestration service for CV generation.
 * Coordinates between Claude API, LaTeX compilation, and caching.
 *
 * @author Pranav Ghorpade
 * @version 1.0
 * @since 2025-01-XX
 *
 * Interview Note:
 * This is the "conductor" of the CV generation process.
 * It follows the Facade pattern, providing a simple interface
 * while coordinating multiple complex subsystems internally.
 */
package com.pranav.cvgenerator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pranav.cvgenerator.model.*;
import com.pranav.cvgenerator.repository.GeneratedCvRepository;
import com.pranav.cvgenerator.util.PromptBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for orchestrating the complete CV generation process.
 *
 * The generation flow:
 * 1. Check cache for existing result
 * 2. Create pending database entry
 * 3. Build Claude prompt with candidate data + JD
 * 4. Call Claude API for LaTeX generation
 * 5. Compile LaTeX to PDF
 * 6. Update database with results
 * 7. Return completed CV
 *
 * Interview Note:
 * This service uses @Async for non-blocking CV generation.
 * The controller returns immediately with a job ID, and clients
 * poll for status until completion.
 */
@Service
@Slf4j
public class CvGenerationService {

    /**
     * Service for Claude API communication.
     */
    private final ClaudeApiService claudeApiService;

    /**
     * Service for LaTeX to PDF compilation.
     */
    private final LatexCompilerService latexCompilerService;

    /**
     * Service for loading candidate profile.
     */
    private final CandidateDataService candidateDataService;

    /**
     * Service for caching results.
     */
    private final CacheService cacheService;

    /**
     * Utility for building Claude prompts.
     */
    private final PromptBuilder promptBuilder;

    /**
     * Repository for storing generated CVs.
     */
    private final GeneratedCvRepository repository;

    /**
     * JSON mapper for serializing coach brief.
     */
    private final ObjectMapper objectMapper;

    /**
     * Service for logging to Google Sheets.
     */
    private final GoogleSheetsService googleSheetsService;

    /**
     * Constructor with dependency injection.
     */
    public CvGenerationService(
            ClaudeApiService claudeApiService,
            LatexCompilerService latexCompilerService,
            CandidateDataService candidateDataService,
            CacheService cacheService,
            PromptBuilder promptBuilder,
            GeneratedCvRepository repository,
            ObjectMapper objectMapper,
            GoogleSheetsService googleSheetsService) {
        this.claudeApiService = claudeApiService;
        this.latexCompilerService = latexCompilerService;
        this.candidateDataService = candidateDataService;
        this.cacheService = cacheService;
        this.promptBuilder = promptBuilder;
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.googleSheetsService = googleSheetsService;
    }

    /**
     * Initiates CV generation and returns immediately with a job ID.
     *
     * This method:
     * 1. Checks cache (returns cached result if available)
     * 2. Creates a pending database entry
     * 3. Starts async generation process
     * 4. Returns the job ID for status polling
     *
     * @param request The generation request with JD
     * @param forceRegenerate If true, skip cache lookup
     * @return UUID of the generation job
     */
    public UUID startGeneration(CvGenerationRequest request, boolean forceRegenerate) {
        String jobDescription = request.getJobDescription();
        String jdHash = cacheService.calculateHash(jobDescription);

        // Check cache unless force regenerate is requested
        if (!forceRegenerate) {
            Optional<GeneratedCv> cached = cacheService.findCachedResult(jobDescription);
            if (cached.isPresent()) {
                log.info("Returning cached CV for JD hash: {}", jdHash);
                return cached.get().getId();
            }
        }

        // Extract company name from JD if not provided
        String companyName = request.getCompanyName();
        if (companyName == null || companyName.isBlank()) {
            companyName = extractCompanyName(jobDescription);
        }

        // Extract job title from JD
        String jobTitle = extractJobTitle(jobDescription);

        // Create pending entry
        GeneratedCv pendingCv = GeneratedCv.builder()
                .jobDescriptionHash(jdHash)
                .jobDescription(jobDescription)
                .companyName(companyName)
                .jobTitle(jobTitle)
                .status(GeneratedCv.GenerationStatus.PROCESSING)
                .build();

        pendingCv = repository.save(pendingCv);
        UUID jobId = pendingCv.getId();

        log.info("Started CV generation job: {} (Experience Level: {})", jobId,
                request.getExperienceLevel() != null ? request.getExperienceLevel().name() : "EXPERIENCED");

        // Start async generation with experience level
        String experienceLevel = request.getExperienceLevel() != null ?
                request.getExperienceLevel().name() : "EXPERIENCED";
        generateCvAsync(jobId, jobDescription, companyName, experienceLevel);

        return jobId;
    }

    /**
     * Asynchronously generates the CV (defaults to EXPERIENCED level).
     *
     * @param jobId The generation job ID
     * @param jobDescription The job description text
     * @param companyName Extracted or provided company name
     */
    @Async("cvGenerationExecutor")
    public void generateCvAsync(UUID jobId, String jobDescription, String companyName) {
        generateCvAsync(jobId, jobDescription, companyName, "EXPERIENCED");
    }

    /**
     * Asynchronously generates the CV with experience level selection.
     *
     * This method runs in a separate thread pool, allowing the
     * main request thread to return immediately.
     *
     * Interview Note:
     * @Async requires @EnableAsync on the configuration class.
     * The method must be public and called from outside the class
     * (Spring proxy limitation).
     *
     * @param jobId The generation job ID
     * @param jobDescription The job description text
     * @param companyName Extracted or provided company name
     * @param experienceLevel ENTRY_LEVEL or EXPERIENCED
     */
    @Async("cvGenerationExecutor")
    public void generateCvAsync(UUID jobId, String jobDescription, String companyName, String experienceLevel) {
        log.info("Starting async CV generation for job: {} (Level: {})", jobId, experienceLevel);
        long startTime = System.currentTimeMillis();

        try {
            // Step 1: Load candidate data
            CandidateProfile candidate = candidateDataService.getProfile();

            // Step 2: Build prompts with experience level
            String systemPrompt = promptBuilder.loadSystemPrompt();
            String userMessage = promptBuilder.buildUserMessage(candidate, jobDescription, experienceLevel);

            // Step 3: Call Claude API
            long claudeStart = System.currentTimeMillis();
            ClaudeApiResponse response = claudeApiService.generateCv(systemPrompt, userMessage);
            long claudeTime = System.currentTimeMillis() - claudeStart;

            if (!response.isSuccess()) {
                throw new RuntimeException("Claude API returned unsuccessful response");
            }

            // Step 4: Compile LaTeX to PDF
            long latexStart = System.currentTimeMillis();
            byte[] pdfBytes = latexCompilerService.compileToPdf(response.getLatexCv());
            long latexTime = System.currentTimeMillis() - latexStart;

            // Step 5: Update database with results
            updateWithSuccess(jobId, response, pdfBytes, claudeTime, latexTime);

            // Step 6: Log to Google Sheets with PDF upload (async, non-blocking)
            try {
                String coachBriefText = response.getCoachBrief() != null ?
                        objectMapper.writeValueAsString(response.getCoachBrief()) : "";
                int matchScore = response.getMatchScore() != null ?
                        response.getMatchScore().getKeywordCoveragePct() : 0;
                String pdfFilename = generatePdfFilename(companyName);
                googleSheetsService.logGenerationWithPdf(
                        companyName,
                        jobDescription,
                        pdfBytes,
                        pdfFilename,
                        coachBriefText,
                        matchScore
                );
            } catch (Exception e) {
                log.warn("Failed to log to Google Sheets: {}", e.getMessage());
            }

            long totalTime = System.currentTimeMillis() - startTime;
            log.info("CV generation completed for job {} in {}ms (Claude: {}ms, LaTeX: {}ms)",
                    jobId, totalTime, claudeTime, latexTime);

        } catch (Exception e) {
            log.error("CV generation failed for job: {}", jobId, e);
            updateWithFailure(jobId, e.getMessage());
        }
    }

    /**
     * Updates the database entry with successful generation results.
     *
     * @param jobId The job ID
     * @param response Claude API response
     * @param pdfBytes Compiled PDF
     * @param claudeTime Claude API processing time
     * @param latexTime LaTeX compilation time
     */
    private void updateWithSuccess(UUID jobId, ClaudeApiResponse response,
                                   byte[] pdfBytes, long claudeTime, long latexTime) {
        GeneratedCv cv = repository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));

        // Set LaTeX and PDF content
        cv.setLatexContent(response.getLatexCv());
        cv.setPdfContent(pdfBytes);

        // Set match scores
        if (response.getMatchScore() != null) {
            cv.setKeywordCoverage(response.getMatchScore().getKeywordCoveragePct());
            cv.setRecruiterFit(response.getMatchScore().getRecruiterFitPct());
        }

        // Set recruiter domain
        if (response.getRecruiterModel() != null) {
            cv.setRecruiterDomain(response.getRecruiterModel().getDomain());
        }

        // Serialize coach brief and keywords to JSON
        try {
            if (response.getCoachBrief() != null) {
                cv.setCoachBriefJson(objectMapper.writeValueAsString(response.getCoachBrief()));
            }
            if (response.getKeywordTiers() != null) {
                cv.setKeywordsJson(objectMapper.writeValueAsString(response.getKeywordTiers()));
            }
        } catch (Exception e) {
            log.warn("Failed to serialize response data", e);
        }

        // Set timing information
        cv.setClaudeProcessingTimeMs(claudeTime);
        cv.setLatexCompilationTimeMs(latexTime);

        // Mark as completed
        cv.setStatus(GeneratedCv.GenerationStatus.COMPLETED);
        cv.setCompletedAt(LocalDateTime.now());

        repository.save(cv);
    }

    /**
     * Updates the database entry with failure information.
     *
     * @param jobId The job ID
     * @param errorMessage Error description
     */
    private void updateWithFailure(UUID jobId, String errorMessage) {
        GeneratedCv cv = repository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));

        cv.setStatus(GeneratedCv.GenerationStatus.FAILED);
        cv.setErrorMessage(errorMessage);
        cv.setCompletedAt(LocalDateTime.now());

        repository.save(cv);
    }

    /**
     * Gets the status and result of a generation job.
     *
     * @param jobId The job ID
     * @return Optional containing the GeneratedCv if found
     */
    public Optional<GeneratedCv> getGenerationResult(UUID jobId) {
        return repository.findById(jobId);
    }

    /**
     * Extracts company name from job description text.
     *
     * Looks for patterns like:
     * - "at Google"
     * - "Company: Microsoft"
     * - "Join Amazon"
     *
     * @param jobDescription The JD text
     * @return Extracted company name or "Company"
     */
    private String extractCompanyName(String jobDescription) {
        // Try various patterns
        String[] patterns = {
            "(?i)(?:at|@|join|company[:\\s]+)([A-Z][A-Za-z0-9\\s&]+?)(?:\\s|,|\\.|\\n|$)",
            "(?i)(?:about|working at)\\s+([A-Z][A-Za-z0-9\\s&]+?)(?:\\s|,|\\.|\\n|$)"
        };

        for (String patternStr : patterns) {
            Pattern pattern = Pattern.compile(patternStr);
            Matcher matcher = pattern.matcher(jobDescription);
            if (matcher.find()) {
                String company = matcher.group(1).trim();
                // Clean up and validate
                if (company.length() > 2 && company.length() < 50) {
                    return sanitizeForFilename(company);
                }
            }
        }

        // Default if no company found
        return "Company";
    }

    /**
     * Extracts job title from job description text.
     *
     * @param jobDescription The JD text
     * @return Extracted job title or "Position"
     */
    private String extractJobTitle(String jobDescription) {
        // Look for common job title patterns
        String[] patterns = {
            "(?i)(?:position|role|title)[:\\s]+([A-Za-z\\s]+?)(?:\\n|,|$)",
            "(?i)^([A-Za-z\\s]+(?:Engineer|Developer|Manager|Analyst|Designer))(?:\\s|\\n|,|$)"
        };

        for (String patternStr : patterns) {
            Pattern pattern = Pattern.compile(patternStr, Pattern.MULTILINE);
            Matcher matcher = pattern.matcher(jobDescription);
            if (matcher.find()) {
                String title = matcher.group(1).trim();
                if (title.length() > 3 && title.length() < 100) {
                    return title;
                }
            }
        }

        return "Position";
    }

    /**
     * Sanitizes a string for use in filenames.
     *
     * Removes or replaces characters that are invalid in filenames.
     *
     * @param input The string to sanitize
     * @return Sanitized string safe for filenames
     */
    private String sanitizeForFilename(String input) {
        if (input == null) return "Unknown";

        // Remove invalid filename characters
        String sanitized = input.replaceAll("[<>:\"/\\\\|?*]", "");

        // Replace spaces with nothing (CamelCase)
        sanitized = sanitized.replaceAll("\\s+", "");

        // Limit length
        if (sanitized.length() > 30) {
            sanitized = sanitized.substring(0, 30);
        }

        return sanitized.isEmpty() ? "Company" : sanitized;
    }

    /**
     * Generates the PDF filename for a CV.
     *
     * Format: PranavGhorpadeCv{CompanyName}.pdf
     *
     * @param companyName The company name
     * @return Formatted filename
     */
    public String generatePdfFilename(String companyName) {
        String sanitized = sanitizeForFilename(companyName);
        return "PranavGhorpadeCv" + sanitized + ".pdf";
    }

    /**
     * Generates the LaTeX filename for a CV.
     *
     * Format: PranavGhorpadeCv{CompanyName}.tex
     *
     * @param companyName The company name
     * @return Formatted filename
     */
    public String generateTexFilename(String companyName) {
        String sanitized = sanitizeForFilename(companyName);
        return "PranavGhorpadeCv" + sanitized + ".tex";
    }
}
