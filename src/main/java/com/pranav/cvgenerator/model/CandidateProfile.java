/**
 * CandidateProfile.java
 *
 * Data model representing the complete candidate profile loaded from
 * candidate-data.json. This class maps the JSON structure to Java objects
 * for use in CV generation.
 *
 * @author Pranav Ghorpade
 */
package com.pranav.cvgenerator.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Root model class for candidate profile data.
 *
 * Contains all information about the candidate including:
 * - Personal details (contact, links)
 * - Professional summary
 * - Education history
 * - Work experience
 * - Technical skills
 * - Certifications
 * - Projects
 * - Personal strengths
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CandidateProfile {

    /**
     * Personal information section containing contact details and social links.
     */
    private PersonalInfo personal;

    /**
     * Professional summary/profile statement.
     * This is the elevator pitch that appears at the top of the CV.
     */
    private String summary;

    /**
     * List of educational qualifications in reverse chronological order.
     */
    private List<Education> education;

    /**
     * List of work experiences in reverse chronological order.
     */
    private List<Experience> experience;

    /**
     * Technical skills categorized by type (languages, frameworks, tools, etc.).
     */
    private Skills skills;

    /**
     * Professional certifications and credentials.
     */
    private List<Certification> certifications;

    /**
     * Personal and professional projects demonstrating practical skills.
     */
    private List<Project> projects;

    /**
     * Personal strengths and soft skills.
     */
    private List<String> strengths;

    // ==================== NESTED CLASSES ====================

    /**
     * Personal contact information and professional links.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PersonalInfo {
        private String name;
        private String location;
        private String email;
        private String phone;
        private String linkedin;
        private String github;
        private String website;
        private String medium;
    }

    /**
     * Educational qualification entry.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Education {
        private String degree;
        private String grade;
        private String institution;
        private String location;
        private String period;
        private List<String> modules;
        private String project;
    }

    /**
     * Work experience entry with role details and achievements.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Experience {
        private String title;
        private String company;
        private String location;
        private String period;
        private List<String> highlights;
        private String note;
    }

    /**
     * Technical skills organized by category.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Skills {
        private SkillCategory languages;
        private SkillCategory frontend;
        private SkillCategory backend;
        private SkillCategory databases;
        private SkillCategory cicd;
        private SkillCategory testing;

        @JsonProperty("cloud_ops")
        private SkillCategory cloudOps;

        @JsonProperty("security_tools")
        private SkillCategory securityTools;

        @JsonProperty("other_tools")
        private List<String> otherTools;

        private SkillCategory practices;
    }

    /**
     * Generic skill category with proficiency levels.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SkillCategory {
        private List<String> proficient;
        private List<String> familiar;
        private List<String> basics;
        private List<String> learning;
        private List<String> exposure;
        private List<String> transferable;
        private List<String> frameworks;
        private List<String> concepts;
        private List<String> other;
        private String note;
    }

    /**
     * Professional certification entry.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Certification {
        private String name;
        private String issuer;
        private List<String> details;
        private String status;
    }

    /**
     * Project entry with technical details and links.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Project {
        private String name;
        private List<String> tech;
        private String description;
        private String github;
        private String note;
    }
}
