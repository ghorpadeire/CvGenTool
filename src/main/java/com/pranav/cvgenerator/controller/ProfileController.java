package com.pranav.cvgenerator.controller;

import com.pranav.cvgenerator.service.CandidateDataService;
import com.pranav.cvgenerator.util.PromptBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

/**
 * Controller for the profile editing page.
 * Provides both the MVC page and REST endpoints for reading/saving
 * the candidate-data.json and cv-gen-system-prompt.txt files.
 *
 * @author Pranav Ghorpade
 */
@Controller
@Slf4j
public class ProfileController {

    private final CandidateDataService candidateDataService;
    private final PromptBuilder promptBuilder;

    public ProfileController(CandidateDataService candidateDataService, PromptBuilder promptBuilder) {
        this.candidateDataService = candidateDataService;
        this.promptBuilder = promptBuilder;
    }

    /** Serves the profile editing page. */
    @GetMapping("/profile")
    public String profilePage(Model model) {
        model.addAttribute("candidateName", candidateDataService.getCandidateName());
        return "profile";
    }

    // ---- REST endpoints for AJAX saves ----

    @GetMapping("/api/profile/candidate-data")
    @ResponseBody
    public ResponseEntity<String> getCandidateData() {
        try {
            return ResponseEntity.ok(candidateDataService.getProfileRawJson());
        } catch (IOException e) {
            log.error("Failed to read candidate data", e);
            return ResponseEntity.internalServerError().body("Failed to read candidate data");
        }
    }

    @PutMapping("/api/profile/candidate-data")
    @ResponseBody
    public ResponseEntity<Map<String, String>> saveCandidateData(@RequestBody String json) {
        try {
            candidateDataService.saveProfileJson(json);
            return ResponseEntity.ok(Map.of(
                    "status", "ok",
                    "name", candidateDataService.getCandidateName()));
        } catch (Exception e) {
            log.error("Failed to save candidate data", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()));
        }
    }

    @GetMapping("/api/profile/system-prompt")
    @ResponseBody
    public ResponseEntity<String> getSystemPrompt() {
        try {
            return ResponseEntity.ok(promptBuilder.getSystemPromptRaw());
        } catch (IOException e) {
            log.error("Failed to read system prompt", e);
            return ResponseEntity.internalServerError().body("Failed to read system prompt");
        }
    }

    @PutMapping("/api/profile/system-prompt")
    @ResponseBody
    public ResponseEntity<Map<String, String>> saveSystemPrompt(@RequestBody String content) {
        try {
            promptBuilder.saveSystemPrompt(content);
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (Exception e) {
            log.error("Failed to save system prompt", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()));
        }
    }
}
