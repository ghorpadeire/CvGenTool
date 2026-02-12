/**
 * CvGenerationController.java
 *
 * REST API controller for CV generation operations.
 * Handles job submission, status checking, and result retrieval.
 *
 * @author Pranav Ghorpade
 */
package com.pranav.cvgenerator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pranav.cvgenerator.model.*;
import com.pranav.cvgenerator.repository.GeneratedCvRepository;
import com.pranav.cvgenerator.service.CvGenerationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

/**
 * REST API controller for CV generation.
 *
 * Endpoints:
 * - POST /api/generate        → Start CV generation
 * - GET  /api/status/{id}     → Check generation status
 * - GET  /api/result/{id}     → Get generation result
 * - DELETE /api/cache/{id}    → Clear cached result
 */
@RestController
@RequestMapping("/api")
@Slf4j
public class CvGenerationController {

    /**
     * Service for CV generation orchestration.
     */
    private final CvGenerationService generationService;

    /**
     * Repository for direct CV queries.
     */
    private final GeneratedCvRepository repository;

    /**
     * JSON mapper for response transformation.
     */
    private final ObjectMapper objectMapper;

    /**
     * Constructor with dependency injection.
     */
    public CvGenerationController(
            CvGenerationService generationService,
            GeneratedCvRepository repository,
            ObjectMapper objectMapper) {
        this.generationService = generationService;
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * Starts a new CV generation job.
     *
     * Process:
     * 1. Validate the request
     * 2. Check cache for existing result
     * 3. Create new generation job
     * 4. Return job ID for status polling
     *
     * Example request:
     * POST /api/generate
     * {
     *   "jobDescription": "We are looking for a Senior Java Developer...",
     *   "companyName": "Google"
     * }
     *
     * Example response:
     * {
     *   "id": "550e8400-e29b-41d4-a716-446655440000",
     *   "status": "PROCESSING"
     * }
     *
     * @param request The generation request with job description
     * @return Response with job ID and initial status
     */
    @PostMapping("/generate")
    public ResponseEntity<CvGenerationResponse> generate(
            @Valid @RequestBody CvGenerationRequest request) {

        log.info("Received CV generation request, JD length: {} chars",
                request.getJobDescription().length());

        try {
            // Start the generation (may return cached result)
            UUID jobId = generationService.startGeneration(
                    request,
                    request.isForceRegenerate()
            );

            // Check if it was a cache hit (already completed)
            Optional<GeneratedCv> cv = repository.findById(jobId);
            if (cv.isPresent() && cv.get().isCompleted()) {
                // Return full result for cache hit
                return ResponseEntity.ok(buildSuccessResponse(cv.get()));
            }

            // Return processing status for new job
            CvGenerationResponse response = CvGenerationResponse.processing(jobId.toString());
            return ResponseEntity.accepted().body(response);

        } catch (Exception e) {
            log.error("Failed to start CV generation", e);
            CvGenerationResponse errorResponse = CvGenerationResponse.failed(
                    null, "Failed to start generation: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    /**
     * Gets the status of a generation job.
     *
     * Example response (processing):
     * {
     *   "id": "550e8400...",
     *   "status": "PROCESSING",
     *   "progress": 50,
     *   "currentStep": "Generating LaTeX CV..."
     * }
     *
     * Example response (completed):
     * {
     *   "id": "550e8400...",
     *   "status": "COMPLETED",
     *   "pdfUrl": "/api/download/550e.../pdf",
     *   "texUrl": "/api/download/550e.../tex"
     * }
     *
     * @param id The generation job UUID
     * @return Response with current status
     */
    @GetMapping("/status/{id}")
    public ResponseEntity<CvGenerationResponse> getStatus(@PathVariable String id) {
        log.debug("Status check for job: {}", id);

        try {
            UUID uuid = UUID.fromString(id);
            Optional<GeneratedCv> cvOpt = repository.findById(uuid);

            if (cvOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            GeneratedCv cv = cvOpt.get();
            CvGenerationResponse response = buildStatusResponse(cv);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID: {}", id);
            return ResponseEntity.badRequest()
                    .body(CvGenerationResponse.failed(id, "Invalid job ID"));
        }
    }

    /**
     * Gets the full result of a completed generation job.
     *
     * Returns all data including:
     * - Match scores
     * - Detected keywords
     * - Keyword tiers
     * - Coach brief
     * - Download URLs
     *
     * @param id The generation job UUID
     * @return Full result response
     */
    @GetMapping("/result/{id}")
    public ResponseEntity<CvGenerationResponse> getResult(@PathVariable String id) {
        log.debug("Result request for job: {}", id);

        try {
            UUID uuid = UUID.fromString(id);
            Optional<GeneratedCv> cvOpt = repository.findById(uuid);

            if (cvOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            GeneratedCv cv = cvOpt.get();

            if (!cv.isCompleted()) {
                // Return status-only response if not complete
                return ResponseEntity.ok(buildStatusResponse(cv));
            }

            // Return full result
            return ResponseEntity.ok(buildSuccessResponse(cv));

        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID: {}", id);
            return ResponseEntity.badRequest()
                    .body(CvGenerationResponse.failed(id, "Invalid job ID"));
        }
    }

    /**
     * Deletes a cached CV result.
     *
     * Useful for:
     * - Forcing regeneration
     * - Cleaning up old results
     *
     * @param id The CV UUID to delete
     * @return Empty response with appropriate status
     */
    @DeleteMapping("/cache/{id}")
    public ResponseEntity<Void> deleteCache(@PathVariable String id) {
        log.info("Delete request for CV: {}", id);

        try {
            UUID uuid = UUID.fromString(id);
            if (repository.existsById(uuid)) {
                repository.deleteById(uuid);
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID: {}", id);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Builds a status-only response for a CV.
     *
     * @param cv The GeneratedCv entity
     * @return Response with status information
     */
    private CvGenerationResponse buildStatusResponse(GeneratedCv cv) {
        CvGenerationResponse.CvGenerationResponseBuilder builder =
                CvGenerationResponse.builder()
                        .id(cv.getId().toString())
                        .status(cv.getStatus().name())
                        .createdAt(cv.getCreatedAt());

        if (cv.isProcessing()) {
            // Estimate progress based on time elapsed
            builder.progress(estimateProgress(cv));
            builder.currentStep("Generating CV...");
        } else if (cv.isFailed()) {
            builder.errorMessage(cv.getErrorMessage());
        } else if (cv.isCompleted()) {
            builder.completedAt(cv.getCompletedAt());
            builder.pdfUrl("/api/download/" + cv.getId() + "/pdf");
            builder.texUrl("/api/download/" + cv.getId() + "/tex");
        }

        return builder.build();
    }

    /**
     * Builds a full success response with all data.
     *
     * @param cv The completed GeneratedCv entity
     * @return Full response with all analysis data
     */
    private CvGenerationResponse buildSuccessResponse(GeneratedCv cv) {
        CvGenerationResponse.CvGenerationResponseBuilder builder =
                CvGenerationResponse.builder()
                        .id(cv.getId().toString())
                        .status("COMPLETED")
                        .companyName(cv.getCompanyName())
                        .jobTitle(cv.getJobTitle())
                        .recruiterDomain(cv.getRecruiterDomain())
                        .createdAt(cv.getCreatedAt())
                        .completedAt(cv.getCompletedAt())
                        .pdfUrl("/api/download/" + cv.getId() + "/pdf")
                        .texUrl("/api/download/" + cv.getId() + "/tex");

        // Add match scores
        if (cv.getKeywordCoverage() != null && cv.getRecruiterFit() != null) {
            builder.matchScore(CvGenerationResponse.MatchScore.builder()
                    .keywordCoverage(cv.getKeywordCoverage())
                    .recruiterFit(cv.getRecruiterFit())
                    .build());
        }

        // Parse and add coach brief from JSON
        if (cv.getCoachBriefJson() != null) {
            try {
                ClaudeApiResponse.CoachBrief parsed = objectMapper.readValue(
                        cv.getCoachBriefJson(),
                        ClaudeApiResponse.CoachBrief.class);

                CvGenerationResponse.LearningRoadmap roadmap = null;
                if (parsed.getLearningRoadmap() != null) {
                    roadmap = CvGenerationResponse.LearningRoadmap.builder()
                            .sevenDays(parsed.getLearningRoadmap().getSevenDays())
                            .fourteenDays(parsed.getLearningRoadmap().getFourteenDays())
                            .twentyOneDays(parsed.getLearningRoadmap().getTwentyOneDays())
                            .build();
                }

                builder.coachBrief(CvGenerationResponse.CoachBrief.builder()
                        .skillGaps(parsed.getSkillGaps())
                        .learningRoadmap(roadmap)
                        .interviewQuestions(parsed.getInterviewQuestions())
                        .build());
            } catch (Exception e) {
                log.warn("Failed to parse coach brief JSON", e);
            }
        }

        // Parse and add keyword tiers from JSON
        if (cv.getKeywordsJson() != null) {
            try {
                ClaudeApiResponse.KeywordTiers parsed = objectMapper.readValue(
                        cv.getKeywordsJson(),
                        ClaudeApiResponse.KeywordTiers.class);

                builder.keywordTiers(CvGenerationResponse.KeywordTiers.builder()
                        .proficient(parsed.getProficient())
                        .exposure(parsed.getExposure())
                        .awareness(parsed.getAwareness())
                        .build());
            } catch (Exception e) {
                log.warn("Failed to parse keywords JSON", e);
            }
        }

        return builder.build();
    }

    /**
     * Estimates generation progress based on elapsed time.
     *
     * Typical timing:
     * - 0-5 sec: 10-30% (analyzing JD)
     * - 5-15 sec: 30-70% (generating LaTeX)
     * - 15-25 sec: 70-90% (compiling PDF)
     * - 25+ sec: 90-99% (finishing up)
     *
     * @param cv The CV being generated
     * @return Estimated progress percentage
     */
    private int estimateProgress(GeneratedCv cv) {
        if (cv.getCreatedAt() == null) {
            return 0;
        }

        long elapsedMs = java.time.Duration.between(
                cv.getCreatedAt(),
                java.time.LocalDateTime.now()
        ).toMillis();

        // Estimate based on typical 20-30 second generation time
        if (elapsedMs < 5000) {
            return (int) (10 + (elapsedMs / 250)); // 10-30%
        } else if (elapsedMs < 15000) {
            return (int) (30 + ((elapsedMs - 5000) / 250)); // 30-70%
        } else if (elapsedMs < 25000) {
            return (int) (70 + ((elapsedMs - 15000) / 500)); // 70-90%
        } else {
            return Math.min(99, 90 + (int) ((elapsedMs - 25000) / 1000)); // 90-99%
        }
    }
}
