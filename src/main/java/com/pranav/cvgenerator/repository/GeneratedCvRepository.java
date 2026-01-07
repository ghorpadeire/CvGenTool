/**
 * GeneratedCvRepository.java
 *
 * JPA Repository for GeneratedCv entities.
 * Provides CRUD operations and custom queries for CV persistence.
 *
 * @author Pranav Ghorpade
 * @version 1.0
 * @since 2025-01-XX
 *
 * Interview Note:
 * Spring Data JPA repositories automatically implement common database
 * operations based on method naming conventions. This eliminates
 * boilerplate code while maintaining type safety.
 */
package com.pranav.cvgenerator.repository;

import com.pranav.cvgenerator.model.GeneratedCv;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for GeneratedCv entity operations.
 *
 * Extends JpaRepository which provides:
 * - save(), saveAll()
 * - findById(), findAll()
 * - deleteById(), delete()
 * - count(), existsById()
 *
 * Custom methods use Spring Data JPA query derivation:
 * - Method names are parsed to create queries automatically
 * - Example: findByStatus -> SELECT * WHERE status = ?
 *
 * Interview Note:
 * Repositories are the Data Access Layer (DAL) in Spring applications.
 * They abstract away the database implementation details, making
 * the service layer database-agnostic.
 */
@Repository
public interface GeneratedCvRepository extends JpaRepository<GeneratedCv, UUID> {

    /**
     * Finds a CV by job description hash and status.
     *
     * Used for cache lookup - finds completed generation for a given JD.
     *
     * Query derivation:
     * "findBy" + "JobDescriptionHash" + "And" + "Status"
     * → SELECT * FROM generated_cvs WHERE jd_hash = ? AND status = ?
     *
     * @param hash MD5 hash of the job description
     * @param status Generation status to match
     * @return Optional containing the CV if found
     */
    Optional<GeneratedCv> findByJobDescriptionHashAndStatus(
            String hash,
            GeneratedCv.GenerationStatus status);

    /**
     * Finds all CVs with a specific status.
     *
     * Useful for:
     * - Finding all completed CVs for the history page
     * - Finding all processing CVs to check for stuck jobs
     *
     * @param status The status to filter by
     * @return List of matching CVs
     */
    List<GeneratedCv> findByStatus(GeneratedCv.GenerationStatus status);

    /**
     * Finds recent completed CVs, ordered by creation date (newest first).
     *
     * Used for the "Recent Generations" display on the home page.
     *
     * Interview Note:
     * "OrderByCreatedAtDesc" is parsed as ORDER BY created_at DESC.
     * Spring Data JPA supports complex ordering clauses.
     *
     * @return List of completed CVs, most recent first
     */
    List<GeneratedCv> findByStatusOrderByCreatedAtDesc(GeneratedCv.GenerationStatus status);

    /**
     * Finds the most recent N completed CVs.
     *
     * Uses JPQL (Java Persistence Query Language) for more complex query.
     *
     * @param status Status to filter
     * @param limit Maximum results to return
     * @return List of recent completed CVs
     */
    @Query("SELECT cv FROM GeneratedCv cv WHERE cv.status = :status " +
           "ORDER BY cv.createdAt DESC LIMIT :limit")
    List<GeneratedCv> findRecentByStatus(
            @Param("status") GeneratedCv.GenerationStatus status,
            @Param("limit") int limit);

    /**
     * Counts CVs with a specific status.
     *
     * Useful for cache statistics and monitoring.
     *
     * @param status Status to count
     * @return Number of CVs with that status
     */
    long countByStatus(GeneratedCv.GenerationStatus status);

    /**
     * Deletes CVs created before a specified time.
     *
     * Used for cache cleanup - removes expired entries.
     *
     * Interview Note:
     * @Modifying annotation is required for UPDATE/DELETE queries.
     * @Transactional ensures the operation is atomic.
     *
     * @param cutoff Delete entries created before this time
     * @return Number of entries deleted
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM GeneratedCv cv WHERE cv.createdAt < :cutoff")
    int deleteByCreatedAtBefore(@Param("cutoff") LocalDateTime cutoff);

    /**
     * Finds CVs for a specific company (case-insensitive).
     *
     * Useful for finding previous CVs generated for the same company.
     *
     * @param companyName Company name to search (case-insensitive)
     * @return List of matching CVs
     */
    List<GeneratedCv> findByCompanyNameIgnoreCaseOrderByCreatedAtDesc(String companyName);

    /**
     * Checks if a cached result exists for a job description hash.
     *
     * Faster than retrieving the full entity when you only need
     * to check existence.
     *
     * @param hash The JD hash to check
     * @param status Status to match
     * @return true if a matching entry exists
     */
    boolean existsByJobDescriptionHashAndStatus(
            String hash,
            GeneratedCv.GenerationStatus status);

    /**
     * Finds CVs that are stuck in processing state.
     *
     * A CV is "stuck" if it's been processing for more than the timeout.
     * This helps identify failed async jobs.
     *
     * @param status Should be PROCESSING
     * @param cutoff CVs created before this time are considered stuck
     * @return List of potentially stuck CVs
     */
    @Query("SELECT cv FROM GeneratedCv cv WHERE cv.status = :status " +
           "AND cv.createdAt < :cutoff")
    List<GeneratedCv> findStuckJobs(
            @Param("status") GeneratedCv.GenerationStatus status,
            @Param("cutoff") LocalDateTime cutoff);
}
