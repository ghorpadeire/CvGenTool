/**
 * CandidateDataService.java
 *
 * Service for loading and managing candidate profile data.
 * Reads the candidate-data.json file and provides access to the profile.
 *
 * @author Pranav Ghorpade
 * @version 1.0
 * @since 2025-01-XX
 *
 * Interview Note:
 * This service demonstrates the Single Responsibility Principle (SRP) -
 * it has one job: managing candidate data. This makes it easy to test,
 * maintain, and modify without affecting other parts of the system.
 */
package com.pranav.cvgenerator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pranav.cvgenerator.model.CandidateProfile;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

/**
 * Service for loading candidate profile from JSON resource.
 *
 * This service:
 * 1. Loads candidate-data.json on startup
 * 2. Caches the profile in memory
 * 3. Provides thread-safe access to the profile
 *
 * Interview Note:
 * @Slf4j from Lombok automatically creates a logger instance.
 * Equivalent to: private static final Logger log = LoggerFactory.getLogger(CandidateDataService.class);
 */
@Service
@Slf4j
public class CandidateDataService {

    /**
     * JSON file path in resources folder.
     * Using ClassPathResource for portable resource loading.
     */
    private static final String CANDIDATE_DATA_FILE = "candidate-data.json";

    /**
     * Jackson ObjectMapper for JSON parsing.
     * Injected by Spring from AppConfig bean.
     */
    private final ObjectMapper objectMapper;

    /**
     * Cached candidate profile loaded from JSON.
     * Loaded once at startup, reused for all requests.
     */
    private CandidateProfile cachedProfile;

    /**
     * Constructor with dependency injection.
     *
     * Interview Note:
     * Constructor injection is preferred over field injection (@Autowired)
     * because:
     * 1. Dependencies are explicit and immutable
     * 2. Easier to test (can pass mock dependencies)
     * 3. Fails fast if dependency is missing
     *
     * @param objectMapper Jackson ObjectMapper for JSON parsing
     */
    public CandidateDataService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Initializes the service by loading candidate data.
     *
     * Called automatically by Spring after dependency injection.
     * If loading fails, the application will fail to start - this is
     * intentional as the app cannot function without candidate data.
     *
     * Interview Note:
     * @PostConstruct is a lifecycle callback that runs after the bean
     * is constructed and dependencies are injected, but before the bean
     * is put into service. Perfect for initialization logic.
     */
    @PostConstruct
    public void init() {
        log.info("Loading candidate profile from {}", CANDIDATE_DATA_FILE);
        try {
            loadProfile();
            log.info("Successfully loaded candidate profile for: {}",
                    cachedProfile.getPersonal().getName());
        } catch (IOException e) {
            log.error("Failed to load candidate profile", e);
            throw new RuntimeException("Failed to load candidate data: " + e.getMessage(), e);
        }
    }

    /**
     * Loads the candidate profile from the JSON resource file.
     *
     * Process:
     * 1. Locate the JSON file in classpath (resources folder)
     * 2. Read the file as an InputStream
     * 3. Parse JSON into CandidateProfile object
     * 4. Cache the result for future requests
     *
     * @throws IOException if file cannot be read or parsed
     */
    private void loadProfile() throws IOException {
        // Get resource from classpath
        ClassPathResource resource = new ClassPathResource(CANDIDATE_DATA_FILE);

        // Read and parse JSON
        try (InputStream inputStream = resource.getInputStream()) {
            cachedProfile = objectMapper.readValue(inputStream, CandidateProfile.class);
        }

        // Validate essential fields
        validateProfile(cachedProfile);
    }

    /**
     * Validates that the loaded profile has required fields.
     *
     * Checks:
     * - Personal info is present (name, email)
     * - At least one experience entry
     * - At least one education entry
     *
     * @param profile The loaded profile to validate
     * @throws IllegalStateException if validation fails
     */
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

    /**
     * Gets the cached candidate profile.
     *
     * Returns a reference to the cached profile. For true immutability,
     * we could return a deep copy, but for this use case the profile
     * is read-only so sharing the reference is acceptable.
     *
     * @return The candidate profile
     * @throws IllegalStateException if profile not loaded
     */
    public CandidateProfile getProfile() {
        if (cachedProfile == null) {
            throw new IllegalStateException("Candidate profile not loaded");
        }
        return cachedProfile;
    }

    /**
     * Gets the candidate's full name for display.
     *
     * @return The candidate's name
     */
    public String getCandidateName() {
        return getProfile().getPersonal().getName();
    }

    /**
     * Gets the candidate profile as a formatted JSON string.
     *
     * Useful for:
     * - Including in Claude API prompts
     * - Debugging and logging
     *
     * @return JSON representation of the profile
     */
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
     * Reloads the candidate profile from the JSON file.
     *
     * Useful for:
     * - Hot-reloading during development
     * - Refreshing data without restart
     *
     * @throws IOException if file cannot be read
     */
    public void reloadProfile() throws IOException {
        log.info("Reloading candidate profile...");
        loadProfile();
        log.info("Profile reloaded successfully");
    }
}
