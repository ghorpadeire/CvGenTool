package com.pranav.cvgenerator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pranav.cvgenerator.model.CandidateProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CandidateDataServiceTest {

    private CandidateDataService service;
    private ObjectMapper objectMapper;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();

        // Create a DataDirectoryService pointing at a temp directory.
        // Its @PostConstruct seeds the default files from classpath.
        DataDirectoryService dataDirectoryService = new DataDirectoryService();
        // Inject the temp dir path via reflection since @Value won't run in a unit test
        var dataDirField = DataDirectoryService.class.getDeclaredField("dataDir");
        dataDirField.setAccessible(true);
        dataDirField.set(dataDirectoryService, tempDir.toString());
        dataDirectoryService.init();

        service = new CandidateDataService(objectMapper, dataDirectoryService);
        service.init();
    }

    @Test
    @DisplayName("Should load candidate profile successfully")
    void shouldLoadCandidateProfile() {
        CandidateProfile profile = service.getProfile();
        assertNotNull(profile, "Profile should not be null");
    }

    @Test
    @DisplayName("Should have correct candidate name")
    void shouldHaveCorrectCandidateName() {
        String name = service.getCandidateName();
        assertEquals("Pranav Prasanna Ghorpade", name,
                "Candidate name should match expected value");
    }

    @Test
    @DisplayName("Should have personal information")
    void shouldHavePersonalInformation() {
        CandidateProfile profile = service.getProfile();
        assertNotNull(profile.getPersonal(), "Personal info should not be null");
        assertNotNull(profile.getPersonal().getEmail(), "Email should not be null");
        assertNotNull(profile.getPersonal().getPhone(), "Phone should not be null");
        assertNotNull(profile.getPersonal().getLinkedin(), "LinkedIn should not be null");
        assertNotNull(profile.getPersonal().getGithub(), "GitHub should not be null");
    }

    @Test
    @DisplayName("Should have education entries")
    void shouldHaveEducationEntries() {
        CandidateProfile profile = service.getProfile();
        assertNotNull(profile.getEducation(), "Education list should not be null");
        assertFalse(profile.getEducation().isEmpty(), "Should have at least one education entry");
    }

    @Test
    @DisplayName("Should have experience entries")
    void shouldHaveExperienceEntries() {
        CandidateProfile profile = service.getProfile();
        assertNotNull(profile.getExperience(), "Experience list should not be null");
        assertFalse(profile.getExperience().isEmpty(), "Should have at least one experience entry");
    }

    @Test
    @DisplayName("Should have skills defined")
    void shouldHaveSkillsDefined() {
        CandidateProfile profile = service.getProfile();
        assertNotNull(profile.getSkills(), "Skills should not be null");
        assertNotNull(profile.getSkills().getLanguages(), "Languages should not be null");
    }

    @Test
    @DisplayName("Should have certifications")
    void shouldHaveCertifications() {
        CandidateProfile profile = service.getProfile();
        assertNotNull(profile.getCertifications(), "Certifications should not be null");
        assertFalse(profile.getCertifications().isEmpty(), "Should have at least one certification");
    }

    @Test
    @DisplayName("Should have projects")
    void shouldHaveProjects() {
        CandidateProfile profile = service.getProfile();
        assertNotNull(profile.getProjects(), "Projects should not be null");
        assertFalse(profile.getProjects().isEmpty(), "Should have at least one project");
    }

    @Test
    @DisplayName("Should return profile as JSON string")
    void shouldReturnProfileAsJson() {
        String json = service.getProfileAsJson();
        assertNotNull(json, "JSON string should not be null");
        assertTrue(json.contains("Pranav"), "JSON should contain candidate name");
        assertTrue(json.contains("education"), "JSON should contain education field");
    }

    @Test
    @DisplayName("Experience should have highlights")
    void experienceShouldHaveHighlights() {
        CandidateProfile profile = service.getProfile();
        var firstExperience = profile.getExperience().get(0);
        assertNotNull(firstExperience.getHighlights(), "Highlights should not be null");
        assertFalse(firstExperience.getHighlights().isEmpty(), "Should have highlights");
    }

    @Test
    @DisplayName("Projects should have tech stack")
    void projectsShouldHaveTechStack() {
        CandidateProfile profile = service.getProfile();
        var firstProject = profile.getProjects().get(0);
        assertNotNull(firstProject.getTech(), "Tech stack should not be null");
        assertNotNull(firstProject.getDescription(), "Description should not be null");
    }
}
