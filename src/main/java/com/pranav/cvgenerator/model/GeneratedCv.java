/**
 * GeneratedCv.java
 *
 * JPA Entity representing a generated CV stored in the database.
 * Contains the LaTeX source, compiled PDF bytes, analysis results,
 * and metadata about the generation.
 *
 * @author Pranav Ghorpade
 */
package com.pranav.cvgenerator.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity class for storing generated CVs in the database.
 *
 * This entity stores:
 * - Generated LaTeX source code
 * - Compiled PDF as byte array
 * - Match scores and analysis from Claude
 * - Coach brief for skill development
 * - Generation metadata (timestamps, status)
 */
@Entity
@Table(name = "generated_cvs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeneratedCv {

    /**
     * Unique identifier for the generated CV.
     * Using UUID for:
     * - No sequential guessing of IDs
     * - Works well in distributed systems
     * - Can be generated client-side if needed
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Hash of the original job description.
     * Used for cache lookup - if same JD is submitted, return cached result.
     */
    @Column(name = "jd_hash", length = 64)
    private String jobDescriptionHash;

    /**
     * Original job description text (truncated for storage).
     * Stored for reference and debugging purposes.
     */
    @Column(name = "job_description", columnDefinition = "CLOB")
    @Lob
    private String jobDescription;

    /**
     * Company name extracted from JD or provided by user.
     * Used for PDF filename: "PranavGhorpadeCv{CompanyName}.pdf"
     */
    @Column(name = "company_name", length = 255)
    private String companyName;

    /**
     * Job title extracted from JD.
     * Used for display and tracking purposes.
     */
    @Column(name = "job_title", length = 255)
    private String jobTitle;

    /**
     * Detected recruiter model/domain.
     * Values: "backend", "devops", "security", "fullstack", "other"
     */
    @Column(name = "recruiter_domain", length = 50)
    private String recruiterDomain;

    /**
     * Generated LaTeX source code.
     * Using CLOB (Character Large Object) for potentially long LaTeX documents.
     */
    @Column(name = "latex_content", columnDefinition = "CLOB")
    @Lob
    private String latexContent;

    /**
     * Compiled PDF as binary data.
     * Using BLOB (Binary Large Object) for the PDF file bytes.
     */
    @Column(name = "pdf_content", columnDefinition = "BLOB")
    @Lob
    private byte[] pdfContent;

    /**
     * Keyword coverage percentage (0-100).
     * Indicates how many JD keywords are matched in the generated CV.
     */
    @Column(name = "keyword_coverage")
    private Integer keywordCoverage;

    /**
     * Recruiter fit percentage (0-100).
     * Overall score indicating how well the CV matches recruiter expectations.
     */
    @Column(name = "recruiter_fit")
    private Integer recruiterFit;

    /**
     * Coach brief JSON containing skill gaps and learning roadmap.
     * Stored as JSON string, parsed when needed.
     */
    @Column(name = "coach_brief", columnDefinition = "CLOB")
    @Lob
    private String coachBriefJson;

    /**
     * Keywords analysis JSON (must_have, nice_to_have, soft_skills).
     * Stored as JSON string for flexible structure.
     */
    @Column(name = "keywords_json", columnDefinition = "CLOB")
    @Lob
    private String keywordsJson;

    /**
     * Generation status.
     * Values: "PROCESSING", "COMPLETED", "FAILED"
     */
    @Column(name = "status", length = 20)
    @Enumerated(EnumType.STRING)
    private GenerationStatus status;

    /**
     * Error message if generation failed.
     */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    /**
     * Timestamp when generation was requested.
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * Timestamp when generation was completed.
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Time taken for Claude API call in milliseconds.
     */
    @Column(name = "claude_processing_time_ms")
    private Long claudeProcessingTimeMs;

    /**
     * Time taken for LaTeX compilation in milliseconds.
     */
    @Column(name = "latex_compilation_time_ms")
    private Long latexCompilationTimeMs;

    /**
     * Lifecycle callback - set creation timestamp before persisting.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = GenerationStatus.PROCESSING;
        }
    }

    /**
     * Enumeration of possible generation statuses.
     */
    public enum GenerationStatus {
        /** CV generation is in progress */
        PROCESSING,
        /** CV generation completed successfully */
        COMPLETED,
        /** CV generation failed with an error */
        FAILED
    }

    /**
     * Helper method to check if generation is complete.
     *
     * @return true if status is COMPLETED
     */
    public boolean isCompleted() {
        return this.status == GenerationStatus.COMPLETED;
    }

    /**
     * Helper method to check if generation failed.
     *
     * @return true if status is FAILED
     */
    public boolean isFailed() {
        return this.status == GenerationStatus.FAILED;
    }

    /**
     * Helper method to check if generation is still processing.
     *
     * @return true if status is PROCESSING
     */
    public boolean isProcessing() {
        return this.status == GenerationStatus.PROCESSING;
    }
}
