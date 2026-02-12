/**
 * HomeController.java
 *
 * Controller for serving the main web pages using Thymeleaf templates.
 * Handles the user interface for CV generation.
 *
 * @author Pranav Ghorpade
 */
package com.pranav.cvgenerator.controller;

import com.pranav.cvgenerator.model.GeneratedCv;
import com.pranav.cvgenerator.repository.GeneratedCvRepository;
import com.pranav.cvgenerator.service.CandidateDataService;
import com.pranav.cvgenerator.service.ClaudeApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Controller for main web pages.
 *
 * Handles routes:
 * - GET /           → Main input page
 * - GET /result/{id} → CV generation result page
 * - GET /history    → Recent generations page
 */
@Controller
@Slf4j
public class HomeController {

    /**
     * Repository for accessing generated CVs.
     */
    private final GeneratedCvRepository repository;

    /**
     * Service for candidate data (for display on pages).
     */
    private final CandidateDataService candidateDataService;

    /**
     * Service for checking Claude API configuration.
     */
    private final ClaudeApiService claudeApiService;

    /**
     * Constructor with dependency injection.
     */
    public HomeController(
            GeneratedCvRepository repository,
            CandidateDataService candidateDataService,
            ClaudeApiService claudeApiService) {
        this.repository = repository;
        this.candidateDataService = candidateDataService;
        this.claudeApiService = claudeApiService;
    }

    /**
     * Serves the main input page.
     *
     * This is the landing page where users:
     * 1. Paste their job description
     * 2. Click "Generate CV"
     * 3. See recent generations
     *
     * @param model Spring MVC model for passing data to template
     * @return Template name "index" → templates/index.html
     */
    @GetMapping("/")
    public String home(Model model) {
        log.debug("Serving home page");

        // Add candidate name for display
        model.addAttribute("candidateName", candidateDataService.getCandidateName());

        // Add recent completed generations (show last 10 on home page)
        List<GeneratedCv> recentCvs = repository.findByStatusOrderByCreatedAtDesc(
                GeneratedCv.GenerationStatus.COMPLETED, PageRequest.of(0, 10));
        model.addAttribute("recentGenerations", recentCvs);

        // Check if Claude API is configured
        boolean apiConfigured = claudeApiService.isConfigured();
        model.addAttribute("apiConfigured", apiConfigured);

        if (!apiConfigured) {
            model.addAttribute("warningMessage",
                    "Claude API key not configured. Set CLAUDE_API_KEY environment variable.");
        }

        return "index";
    }

    /**
     * Serves the CV generation result page.
     *
     * Displays:
     * - Match scores
     * - Detected keywords
     * - PDF preview
     * - Download links
     * - Coach brief
     *
     * @param id The generation job UUID
     * @param model Spring MVC model
     * @return Template name for result or error page
     */
    @GetMapping("/result/{id}")
    public String result(@PathVariable String id, Model model) {
        log.debug("Serving result page for ID: {}", id);

        try {
            UUID uuid = UUID.fromString(id);
            Optional<GeneratedCv> cvOpt = repository.findById(uuid);

            if (cvOpt.isEmpty()) {
                model.addAttribute("errorMessage", "CV not found");
                return "error";
            }

            GeneratedCv cv = cvOpt.get();
            model.addAttribute("cv", cv);
            model.addAttribute("candidateName", candidateDataService.getCandidateName());

            // Check status
            if (cv.isProcessing()) {
                // Redirect to generating page if still processing
                return "generating";
            } else if (cv.isFailed()) {
                model.addAttribute("errorMessage", cv.getErrorMessage());
                return "error";
            }

            // Success - show result page
            return "result";

        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID: {}", id);
            model.addAttribute("errorMessage", "Invalid CV ID");
            return "error";
        }
    }

    /**
     * Serves the generation progress page.
     *
     * This page shows a progress indicator and auto-refreshes
     * until generation is complete.
     *
     * @param id The generation job UUID
     * @param model Spring MVC model
     * @return Template name for generating page
     */
    @GetMapping("/generating/{id}")
    public String generating(@PathVariable String id, Model model) {
        log.debug("Serving generating page for ID: {}", id);

        try {
            UUID uuid = UUID.fromString(id);
            Optional<GeneratedCv> cvOpt = repository.findById(uuid);

            if (cvOpt.isEmpty()) {
                model.addAttribute("errorMessage", "CV not found");
                return "error";
            }

            GeneratedCv cv = cvOpt.get();
            model.addAttribute("cv", cv);
            model.addAttribute("jobId", id);

            // If already complete, redirect to result
            if (cv.isCompleted()) {
                return "redirect:/result/" + id;
            } else if (cv.isFailed()) {
                model.addAttribute("errorMessage", cv.getErrorMessage());
                return "error";
            }

            return "generating";

        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID: {}", id);
            model.addAttribute("errorMessage", "Invalid CV ID");
            return "error";
        }
    }

    /**
     * Serves the generation history page.
     *
     * Shows all previously generated CVs with:
     * - Company name
     * - Job title
     * - Match score
     * - Generation date
     * - Download links
     *
     * @param model Spring MVC model
     * @return Template name for history page
     */
    @GetMapping("/history")
    public String history(Model model) {
        log.debug("Serving history page");

        List<GeneratedCv> allCvs = repository.findByStatusOrderByCreatedAtDesc(
                GeneratedCv.GenerationStatus.COMPLETED);

        model.addAttribute("generations", allCvs);
        model.addAttribute("candidateName", candidateDataService.getCandidateName());

        return "history";
    }

    /**
     * Serves the coach brief page for a specific CV.
     *
     * Shows detailed learning roadmap and interview prep.
     *
     * @param id The generation job UUID
     * @param model Spring MVC model
     * @return Template name for coach brief page
     */
    @GetMapping("/coach/{id}")
    public String coachBrief(@PathVariable String id, Model model) {
        log.debug("Serving coach brief page for ID: {}", id);

        try {
            UUID uuid = UUID.fromString(id);
            Optional<GeneratedCv> cvOpt = repository.findById(uuid);

            if (cvOpt.isEmpty()) {
                model.addAttribute("errorMessage", "CV not found");
                return "error";
            }

            GeneratedCv cv = cvOpt.get();
            if (!cv.isCompleted()) {
                model.addAttribute("errorMessage", "CV generation not complete");
                return "error";
            }

            model.addAttribute("cv", cv);
            model.addAttribute("candidateName", candidateDataService.getCandidateName());

            return "coach";

        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID: {}", id);
            model.addAttribute("errorMessage", "Invalid CV ID");
            return "error";
        }
    }
}
