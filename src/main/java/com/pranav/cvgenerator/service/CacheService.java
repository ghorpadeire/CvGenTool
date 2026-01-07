/**
 * CacheService.java
 *
 * Service for caching generated CVs to reduce API costs and response time.
 * Uses MD5 hash of job description to identify duplicate requests.
 *
 * @author Pranav Ghorpade
 * @version 1.0
 * @since 2025-01-XX
 *
 * Interview Note:
 * Caching is crucial for cost optimization when using paid APIs like Claude.
 * If a user submits the same JD multiple times, we return the cached result
 * instead of making another expensive API call.
 */
package com.pranav.cvgenerator.service;

import com.pranav.cvgenerator.model.GeneratedCv;
import com.pranav.cvgenerator.repository.GeneratedCvRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for managing CV generation cache.
 *
 * Cache Strategy:
 * 1. Hash the incoming job description
 * 2. Check if a cached result exists for this hash
 * 3. If cached and not expired, return cached result
 * 4. If not cached or expired, generate new CV
 * 5. Store new result in cache
 *
 * Interview Note:
 * This is a simple database-backed cache. For production at scale,
 * consider using Redis or Memcached for better performance.
 */
@Service
@Slf4j
public class CacheService {

    /**
     * Cache time-to-live in milliseconds.
     * Default: 24 hours (86400000ms)
     */
    @Value("${cache.cv.ttl:86400000}")
    private long cacheTtlMs;

    /**
     * Repository for accessing cached CVs.
     */
    private final GeneratedCvRepository repository;

    /**
     * Constructor with dependency injection.
     *
     * @param repository JPA repository for GeneratedCv entities
     */
    public CacheService(GeneratedCvRepository repository) {
        this.repository = repository;
    }

    /**
     * Looks up a cached CV result for the given job description.
     *
     * Process:
     * 1. Calculate MD5 hash of the JD
     * 2. Query database for matching hash
     * 3. Check if result is within TTL
     * 4. Return cached result if valid
     *
     * @param jobDescription The job description to look up
     * @return Optional containing cached CV if found and valid
     */
    public Optional<GeneratedCv> findCachedResult(String jobDescription) {
        String hash = calculateHash(jobDescription);
        log.debug("Looking up cache for JD hash: {}", hash);

        // Find completed generations with matching hash
        Optional<GeneratedCv> cached = repository.findByJobDescriptionHashAndStatus(
                hash, GeneratedCv.GenerationStatus.COMPLETED);

        if (cached.isPresent()) {
            GeneratedCv cv = cached.get();

            // Check if cache entry is still valid (within TTL)
            if (isWithinTtl(cv.getCreatedAt())) {
                log.info("Cache hit for JD hash: {}", hash);
                return cached;
            } else {
                log.info("Cache expired for JD hash: {}", hash);
                return Optional.empty();
            }
        }

        log.debug("Cache miss for JD hash: {}", hash);
        return Optional.empty();
    }

    /**
     * Checks if a cached entry is within the TTL.
     *
     * @param createdAt Timestamp when the entry was created
     * @return true if entry is still valid
     */
    private boolean isWithinTtl(LocalDateTime createdAt) {
        if (createdAt == null) {
            return false;
        }

        LocalDateTime expiryTime = createdAt.plusNanos(cacheTtlMs * 1_000_000);
        return LocalDateTime.now().isBefore(expiryTime);
    }

    /**
     * Calculates MD5 hash of the job description.
     *
     * MD5 is used because:
     * - Fast to compute
     * - Consistent across platforms
     * - Collision-resistant enough for caching purposes
     *
     * Interview Note:
     * MD5 is not suitable for security purposes (passwords, signatures)
     * but is fine for cache keys where collision resistance is sufficient.
     *
     * @param content The content to hash
     * @return Hex string of MD5 hash
     */
    public String calculateHash(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(content.getBytes(StandardCharsets.UTF_8));

            // Convert bytes to hex string
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (NoSuchAlgorithmException e) {
            // MD5 is always available in Java
            log.error("MD5 algorithm not found", e);
            return String.valueOf(content.hashCode());
        }
    }

    /**
     * Clears expired cache entries.
     *
     * Should be called periodically (e.g., daily) to clean up
     * expired entries and free database space.
     *
     * @return Number of entries removed
     */
    public int clearExpiredEntries() {
        LocalDateTime cutoff = LocalDateTime.now().minusNanos(cacheTtlMs * 1_000_000);
        int count = repository.deleteByCreatedAtBefore(cutoff);
        log.info("Cleared {} expired cache entries", count);
        return count;
    }

    /**
     * Gets cache statistics for monitoring.
     *
     * @return Cache statistics map
     */
    public CacheStats getStats() {
        long totalEntries = repository.count();
        long completedEntries = repository.countByStatus(GeneratedCv.GenerationStatus.COMPLETED);

        return new CacheStats(totalEntries, completedEntries);
    }

    /**
     * Simple record for cache statistics.
     *
     * Interview Note:
     * Java 16+ records are perfect for simple data carriers like this.
     * They automatically generate constructor, getters, equals, hashCode, and toString.
     */
    public record CacheStats(long totalEntries, long completedEntries) {}
}
