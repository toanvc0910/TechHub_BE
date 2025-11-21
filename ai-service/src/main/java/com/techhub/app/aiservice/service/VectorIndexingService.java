package com.techhub.app.aiservice.service;

import com.techhub.app.aiservice.config.QdrantProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service to handle batch indexing of existing courses
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VectorIndexingService {

    private final JdbcTemplate jdbcTemplate;
    private final VectorService vectorService;
    private final QdrantProperties qdrantProperties;

    /**
     * Reindex all existing courses from PostgreSQL into Qdrant
     * This is used when:
     * - AI Service starts for the first time
     * - Qdrant collection was deleted
     * - Need to rebuild embeddings after model change
     */
    public int reindexAllCourses() {
        log.info("📊 Fetching all published courses from database...");

        String sql = "SELECT " +
                "id, title, description, objectives, requirements, categories, tags, " +
                "level, language, instructor_id, status " +
                "FROM courses " +
                "WHERE status = 'PUBLISHED' AND is_active = 'Y' " +
                "ORDER BY created DESC";

        List<Map<String, Object>> courses = jdbcTemplate.queryForList(sql);

        if (courses.isEmpty()) {
            log.warn("⚠️ No published courses found in database");
            return 0;
        }

        log.info("📦 Found {} published courses. Starting batch indexing...", courses.size());

        int successCount = 0;
        int batchSize = 10;

        for (int i = 0; i < courses.size(); i += batchSize) {
            int end = Math.min(i + batchSize, courses.size());
            List<Map<String, Object>> batch = courses.subList(i, end);

            try {
                vectorService.batchIndexCourses(batch);
                successCount += batch.size();
                log.info("✅ Indexed batch {}/{} ({} courses)",
                        (i / batchSize) + 1,
                        (courses.size() + batchSize - 1) / batchSize,
                        batch.size());
            } catch (Exception e) {
                log.error("❌ Failed to index batch {}/{}: {}",
                        (i / batchSize) + 1,
                        (courses.size() + batchSize - 1) / batchSize,
                        e.getMessage());
            }
        }

        log.info("🎉 Reindexing completed: {}/{} courses indexed successfully",
                successCount, courses.size());

        return successCount;
    }

    /**
     * Get statistics about Qdrant collections
     */
    public Map<String, Object> getCollectionStats() {
        Map<String, Object> stats = new HashMap<>();

        try {
            // Count courses in PostgreSQL
            Integer dbCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM courses WHERE status = 'PUBLISHED' AND is_active = 'Y'",
                    Integer.class);

            stats.put("database_published_courses", dbCount);
            stats.put("course_embeddings_collection", qdrantProperties.getRecommendationCollection());
            stats.put("user_embeddings_collection", qdrantProperties.getProfileCollection());
            stats.put("qdrant_host", qdrantProperties.getHost());
            stats.put("note", "To get vector counts, query Qdrant directly: GET " + qdrantProperties.getHost()
                    + "/collections/{collection_name}");

        } catch (Exception e) {
            log.error("❌ Failed to get Qdrant stats", e);
            stats.put("error", e.getMessage());
        }

        return stats;
    }
}
