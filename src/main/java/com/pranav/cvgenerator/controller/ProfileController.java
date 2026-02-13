package com.pranav.cvgenerator.controller;

import com.pranav.cvgenerator.service.CandidateDataService;
import com.pranav.cvgenerator.service.DataDirectoryService;
import com.pranav.cvgenerator.util.PromptBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@Controller
@Slf4j
public class ProfileController {

    private final CandidateDataService candidateDataService;
    private final PromptBuilder promptBuilder;
    private final DataDirectoryService dataDirectoryService;

    public ProfileController(CandidateDataService candidateDataService,
                             PromptBuilder promptBuilder,
                             DataDirectoryService dataDirectoryService) {
        this.candidateDataService = candidateDataService;
        this.promptBuilder = promptBuilder;
        this.dataDirectoryService = dataDirectoryService;
    }

    @GetMapping("/profile")
    public String profilePage(Model model) {
        model.addAttribute("candidateName", candidateDataService.getCandidateName());
        return "profile";
    }

    // ---- Experienced endpoints ----

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

    // ---- Fresher endpoints ----

    @GetMapping("/api/profile/candidate-data-fresher")
    @ResponseBody
    public ResponseEntity<String> getCandidateDataFresher() {
        try {
            return ResponseEntity.ok(
                    dataDirectoryService.readString(DataDirectoryService.CANDIDATE_DATA_FRESHER_FILE));
        } catch (IOException e) {
            log.error("Failed to read fresher candidate data", e);
            return ResponseEntity.internalServerError().body("Failed to read fresher candidate data");
        }
    }

    @PutMapping("/api/profile/candidate-data-fresher")
    @ResponseBody
    public ResponseEntity<Map<String, String>> saveCandidateDataFresher(@RequestBody String json) {
        try {
            // Validate JSON syntax
            new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
            dataDirectoryService.writeString(DataDirectoryService.CANDIDATE_DATA_FRESHER_FILE, json);
            return ResponseEntity.ok(Map.of("status", "ok", "name", "Fresher profile"));
        } catch (Exception e) {
            log.error("Failed to save fresher candidate data", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()));
        }
    }

    @GetMapping("/api/profile/system-prompt-fresher")
    @ResponseBody
    public ResponseEntity<String> getSystemPromptFresher() {
        try {
            return ResponseEntity.ok(
                    dataDirectoryService.readString(DataDirectoryService.SYSTEM_PROMPT_FRESHER_FILE));
        } catch (IOException e) {
            log.error("Failed to read fresher system prompt", e);
            return ResponseEntity.internalServerError().body("Failed to read fresher system prompt");
        }
    }

    @PutMapping("/api/profile/system-prompt-fresher")
    @ResponseBody
    public ResponseEntity<Map<String, String>> saveSystemPromptFresher(@RequestBody String content) {
        try {
            if (content == null || content.isBlank()) {
                throw new IllegalArgumentException("Fresher system prompt cannot be blank");
            }
            dataDirectoryService.writeString(DataDirectoryService.SYSTEM_PROMPT_FRESHER_FILE, content);
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (Exception e) {
            log.error("Failed to save fresher system prompt", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()));
        }
    }
}
