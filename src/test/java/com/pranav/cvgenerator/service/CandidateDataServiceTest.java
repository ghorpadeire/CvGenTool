/**
 * CandidateDataServiceTest.java
 *
 * Unit tests for CandidateDataService.
 * Tests the loading and validation of candidate profile data.
 *
 * @author Pranav Ghorpade
 * @version 1.0
 *
 * Interview Note:
 * Unit tests demonstrate code quality and attention to detail.
 * Good tests verify expected behavior and edge cases.
 */
package com.pranav.cvgenerator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pranav.cvgenerator.model.CandidateProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for CandidateDataService.
 *
 * Interview Note:
 * @DisplayName provides human-readable test descriptions
 * that appear in test reports and IDE output.
 */
class CandidateDataServiceTest {

    private CandidateDataService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new CandidateDataService(objectMapper);
        service.init(); // Load the profile
    }

    @Test
    @DisplayName("Should load candidate profile successfully")
    void shouldLoadCandidateProfile() {
        // When
        CandidateProfile profile = service.getProfile();

        // Then
        assertNotNull(profile, "Profile should not be null");
    }

    @Test
    @DisplayName("Should have correct candidate name")
    void shouldHaveCorrectCandidateName() {
        // When
        String name = service.getCandidateName();

        // Then
        assertEquals("Pranav Prasanna Ghorpade", name,
                "Candidate name should match expected value");
    }

    @Test
    @DisplayName("Should have personal information")
    void shouldHavePersonalInformation() {
        // When
        CandidateProfile profile = service.getProfile();

        // Then
        assertNotNull(profile.getPersonal(), "Personal info should not be null");
        assertNotNull(profile.getPersonal().getEmail(), "Email should not be null");
        assertNotNull(profile.getPersonal().getPhone(), "Phone should not be null");
        assertNotNull(profile.getPersonal().getLinkedin(), "LinkedIn should not be null");
        assertNotNull(profile.getPersonal().getGithub(), "GitHub should not be null");
    }

    @Test
    @DisplayName("Should have education entries")
    void shouldHaveEducationEntries() {
        // When
        CandidateProfile profile = service.getProfile();

        // Then
        assertNotNull(profile.getEducation(), "Education list should not be null");
        assertFalse(profile.getEducation().isEmpty(), "Should have at least one education entry");
    }

    @Test
    @DisplayName("Should have experience entries")
    void shouldHaveExperienceEntries() {
        // When
        CandidateProfile profile = service.getProfile();

        // Then
        assertNotNull(profile.getExperience(), "Experience list should not be null");
        assertFalse(profile.getExperience().isEmpty(), "Should have at least one experience entry");
    }

    @Test
    @DisplayName("Should have skills defined")
    void shouldHaveSkillsDefined() {
        // When
        CandidateProfile profile = service.getProfile();

        // Then
        assertNotNull(profile.getSkills(), "Skills should not be null");
        assertNotNull(profile.getSkills().getLanguages(), "Languages should not be null");
    }

    @Test
    @DisplayName("Should have certifications")
    void shouldHaveCertifications() {
        // When
        CandidateProfile profile = service.getProfile();

        // Then
        assertNotNull(profile.getCertifications(), "Certifications should not be null");
        assertFalse(profile.getCertifications().isEmpty(), "Should have at least one certification");
    }

    @Test
    @DisplayName("Should have projects")
    void shouldHaveProjects() {
        // When
        CandidateProfile profile = service.getProfile();

        // Then
        assertNotNull(profile.getProjects(), "Projects should not be null");
        assertFalse(profile.getProjects().isEmpty(), "Should have at least one project");
    }

    @Test
    @DisplayName("Should return profile as JSON string")
    void shouldReturnProfileAsJson() {
        // When
        String json = service.getProfileAsJson();

        // Then
        assertNotNull(json, "JSON string should not be null");
        assertTrue(json.contains("Pranav"), "JSON should contain candidate name");
        assertTrue(json.contains("education"), "JSON should contain education field");
    }

    @Test
    @DisplayName("Experience should have highlights")
    void experienceShouldHaveHighlights() {
        // When
        CandidateProfile profile = service.getProfile();
        var firstExperience = profile.getExperience().get(0);

        // Then
        assertNotNull(firstExperience.getHighlights(), "Highlights should not be null");
        assertFalse(firstExperience.getHighlights().isEmpty(), "Should have highlights");
    }

    @Test
    @DisplayName("Projects should have tech stack")
    void projectsShouldHaveTechStack() {
        // When
        CandidateProfile profile = service.getProfile();
        var firstProject = profile.getProjects().get(0);

        // Then
        assertNotNull(firstProject.getTech(), "Tech stack should not be null");
        assertNotNull(firstProject.getDescription(), "Description should not be null");
    }
}
