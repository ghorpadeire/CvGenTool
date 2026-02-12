package com.pranav.cvgenerator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pranav.cvgenerator.model.CandidateProfile;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Service for loading and managing candidate profile data.
 * Reads from the writable data directory (falls back to classpath defaults).
 *
 * @author Pranav Ghorpade
 */
@Service
@Slf4j
public class CandidateDataService {

    private final ObjectMapper objectMapper;
    private final DataDirectoryService dataDirectoryService;

    private CandidateProfile cachedProfile;

    public CandidateDataService(ObjectMapper objectMapper, DataDirectoryService dataDirectoryService) {
        this.objectMapper = objectMapper;
        this.dataDirectoryService = dataDirectoryService;
    }

    @PostConstruct
    public void init() {
        log.info("Loading candidate profile from writable data directory");
        try {
            loadProfile();
            log.info("Successfully loaded candidate profile for: {}",
                    cachedProfile.getPersonal().getName());
        } catch (IOException e) {
            log.error("Failed to load candidate profile", e);
            throw new RuntimeException("Failed to load candidate data: " + e.getMessage(), e);
        }
    }

    private void loadProfile() throws IOException {
        String json = dataDirectoryService.readString(DataDirectoryService.CANDIDATE_DATA_FILE);
        cachedProfile = objectMapper.readValue(json, CandidateProfile.class);
        validateProfile(cachedProfile);
    }

    private void validateProfile(CandidateProfile profile) {
        if (profile == null) {
            throw new IllegalStateException("Candidate profile is null");
        }
        if (profile.getPersonal() == null || profile.getPersonal().getName() == null) {
            throw new IllegalStateException("Candidate name is required");
        }
        if (profile.getExperience() == null || profile.getExperience().isEmpty()) {
            throw new IllegalStateException("At least one experience entry is required");
        }
        if (profile.getEducation() == null || profile.getEducation().isEmpty()) {
            throw new IllegalStateException("At least one education entry is required");
        }
    }

    public CandidateProfile getProfile() {
        if (cachedProfile == null) {
            throw new IllegalStateException("Candidate profile not loaded");
        }
        return cachedProfile;
    }

    public String getCandidateName() {
        return getProfile().getPersonal().getName();
    }

    public String getProfileAsJson() {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(cachedProfile);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("Failed to serialize profile to JSON", e);
            throw new RuntimeException("Failed to serialize profile", e);
        }
    }

    /**
     * Returns the raw JSON string from the data directory file
     * (preserving original formatting for the profile editor).
     */
    public String getProfileRawJson() throws IOException {
        return dataDirectoryService.readString(DataDirectoryService.CANDIDATE_DATA_FILE);
    }

    /**
     * Saves new candidate JSON to the writable data directory
     * and reloads the cached profile.
     */
    public void saveProfileJson(String json) throws IOException {
        // Validate that the JSON parses correctly before saving
        CandidateProfile parsed = objectMapper.readValue(json, CandidateProfile.class);
        validateProfile(parsed);

        dataDirectoryService.writeString(DataDirectoryService.CANDIDATE_DATA_FILE, json);
        cachedProfile = parsed;
        log.info("Saved and reloaded candidate profile for: {}",
                cachedProfile.getPersonal().getName());
    }

    public void reloadProfile() throws IOException {
        log.info("Reloading candidate profile...");
        loadProfile();
        log.info("Profile reloaded successfully");
    }
}
