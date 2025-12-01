package com.techhub.app.aiservice.service;

import com.techhub.app.aiservice.client.QdrantClient;
import com.techhub.app.aiservice.config.QdrantProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service to handle batch indexing of existing courses and lessons
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VectorIndexingService {

    private final JdbcTemplate jdbcTemplate;
    private final VectorService vectorService;
    private final QdrantProperties qdrantProperties;
    private final QdrantClient qdrantClient;
    private final EmbeddingService embeddingService;

    /**
     * Reindex all existing courses from PostgreSQL into Qdrant
     * This method CLEARS the collection first to remove stale/orphaned embeddings
     */
    public int reindexAllCourses() {
        log.info("📊 Starting full course reindexing...");
        
        // Step 1: Clear the collection to remove stale embeddings
        log.info("🗑️ Clearing course embeddings collection to remove stale data...");
        try {
            int dimension = embeddingService.getEmbeddingDimension();
            qdrantClient.clearCollection(qdrantProperties.getRecommendationCollection(), dimension);
            log.info("✅ Collection cleared successfully");
        } catch (Exception e) {
            log.error("❌ Failed to clear collection, continuing with upsert only...", e);
        }

        // Step 2: Fetch all published courses from database
        log.info("📊 Fetching all published courses from database...");

        // Query courses with tags and skills aggregated as JSON arrays
        String sql = "SELECT " +
                "c.id, c.title, c.description, c.objectives, c.requirements, " +
                "c.level, c.language, c.instructor_id, c.status, " +
                "COALESCE(json_agg(DISTINCT t.name) FILTER (WHERE t.name IS NOT NULL), '[]'::json) as tags, " +
                "COALESCE(json_agg(DISTINCT s.name) FILTER (WHERE s.name IS NOT NULL), '[]'::json) as skills " +
                "FROM courses c " +
                "LEFT JOIN course_tags ct ON c.id = ct.course_id " +
                "LEFT JOIN tags t ON ct.tag_id = t.id AND t.is_active = 'Y' " +
                "LEFT JOIN course_skills cs ON c.id = cs.course_id " +
                "LEFT JOIN skills s ON cs.skill_id = s.id AND s.is_active = 'Y' " +
                "WHERE c.status = 'PUBLISHED' AND c.is_active = 'Y' " +
                "GROUP BY c.id, c.title, c.description, c.objectives, c.requirements, " +
                "c.level, c.language, c.instructor_id, c.status " +
                "ORDER BY c.created DESC";

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
     * Reindex all lessons from database to Qdrant
     * This method CLEARS the lesson collection first to remove stale/orphaned embeddings
     */
    @Transactional(readOnly = true)
    public int reindexAllLessons() {
        log.info("🔄 Starting full lesson reindexing...");

        // Step 1: Clear the lesson collection to remove stale embeddings
        log.info("🗑️ Clearing lesson embeddings collection to remove stale data...");
        try {
            int dimension = embeddingService.getEmbeddingDimension();
            qdrantClient.clearCollection(qdrantProperties.getLessonCollection(), dimension);
            log.info("✅ Lesson collection cleared successfully");
        } catch (Exception e) {
            log.error("❌ Failed to clear lesson collection, continuing with upsert only...", e);
        }

        try {
            // Fetch all lessons with course_id via JOIN with chapters table
            String sql = "SELECT l.id, l.title, l.content, l.video_url, l.chapter_id, l.content_type, c.course_id " +
                    "FROM lessons l " +
                    "JOIN chapters c ON l.chapter_id = c.id " +
                    "WHERE l.is_active = 'Y'";

            List<Map<String, Object>> lessons = jdbcTemplate.queryForList(sql);
            log.info("📊 Found {} lessons to reindex", lessons.size());

            int count = 0;
            for (Map<String, Object> lesson : lessons) {
                try {
                    UUID lessonId = (UUID) lesson.get("id");
                    String title = (String) lesson.get("title");
                    String content = (String) lesson.get("content");
                    String videoUrl = (String) lesson.get("video_url");

                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("course_id",
                            lesson.get("course_id") != null ? lesson.get("course_id").toString() : null);
                    metadata.put("chapter_id",
                            lesson.get("chapter_id") != null ? lesson.get("chapter_id").toString() : null);
                    metadata.put("content_type", lesson.get("content_type"));

                    vectorService.indexLesson(lessonId, title, content, videoUrl, metadata);
                    count++;
                } catch (Exception e) {
                    log.error("❌ Failed to index lesson: {}", lesson.get("id"), e);
                }
            }

            log.info("✅ Lesson reindexing completed: {}/{} lessons indexed", count, lessons.size());
            return count;
        } catch (Exception e) {
            log.error("❌ Lesson reindexing failed", e);
            throw new RuntimeException("Lesson reindexing failed", e);
        }
    }

    /**
     * Reindex all enrollments from database to Qdrant
     */
    @Transactional(readOnly = true)
    public int reindexAllEnrollments() {
        log.info("🔄 Starting full enrollment reindexing...");

        try {
            // Calculate average progress per enrollment by joining through chapters and
            // lessons
            String sql = "SELECT e.id, e.user_id, e.course_id, e.status, " +
                    "COALESCE(AVG(p.completion), 0.0) as progress " +
                    "FROM enrollments e " +
                    "LEFT JOIN chapters ch ON ch.course_id = e.course_id " +
                    "LEFT JOIN lessons l ON l.chapter_id = ch.id " +
                    "LEFT JOIN progress p ON p.lesson_id = l.id AND p.user_id = e.user_id " +
                    "WHERE e.is_active = 'Y' " +
                    "GROUP BY e.id, e.user_id, e.course_id, e.status";

            List<Map<String, Object>> enrollments = jdbcTemplate.queryForList(sql);
            log.info("📊 Found {} enrollments to reindex", enrollments.size());

            int count = 0;
            for (Map<String, Object> enrollment : enrollments) {
                try {
                    UUID enrollmentId = (UUID) enrollment.get("id");
                    UUID userId = (UUID) enrollment.get("user_id");
                    UUID courseId = (UUID) enrollment.get("course_id");
                    String status = (String) enrollment.get("status");

                    // Get progress as Double, default to 0.0 if null
                    Object progressObj = enrollment.get("progress");
                    Double progress = 0.0;
                    if (progressObj != null) {
                        if (progressObj instanceof Number) {
                            progress = ((Number) progressObj).doubleValue();
                        }
                    }

                    vectorService.indexEnrollment(enrollmentId, userId, courseId, status, progress);
                    count++;
                } catch (Exception e) {
                    log.error("❌ Failed to index enrollment: {}", enrollment.get("id"), e);
                }
            }

            log.info("✅ Enrollment reindexing completed: {}/{} enrollments indexed", count, enrollments.size());
            return count;
        } catch (Exception e) {
            log.error("❌ Enrollment reindexing failed", e);
            throw new RuntimeException("Enrollment reindexing failed", e);
        }
    }

    /**
     * Reindex everything: Courses, Lessons, Enrollments
     */
    public Map<String, Integer> reindexAll() {
        log.info("🚀 Starting full system reindexing...");

        int courses = reindexAllCourses();
        int lessons = reindexAllLessons();
        int enrollments = reindexAllEnrollments();

        log.info("✨ Full system reindexing completed!");

        return Map.of(
                "courses", courses,
                "lessons", lessons,
                "enrollments", enrollments);
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
            stats.put("lesson_embeddings_collection", qdrantProperties.getLessonCollection());
            stats.put("qdrant_host", qdrantProperties.getHost());

            // Get actual embedding counts from Qdrant
            try {
                Map<String, Object> courseCollectionInfo = qdrantClient.getCollectionInfo(qdrantProperties.getRecommendationCollection());
                if (courseCollectionInfo != null && !courseCollectionInfo.isEmpty()) {
                    stats.put("qdrant_course_embeddings_count", courseCollectionInfo.get("points_count"));
                    stats.put("qdrant_course_collection_status", courseCollectionInfo.get("status"));
                }
                
                Map<String, Object> lessonCollectionInfo = qdrantClient.getCollectionInfo(qdrantProperties.getLessonCollection());
                if (lessonCollectionInfo != null && !lessonCollectionInfo.isEmpty()) {
                    stats.put("qdrant_lesson_embeddings_count", lessonCollectionInfo.get("points_count"));
                }
            } catch (Exception qdrantEx) {
                log.warn("⚠️ Could not fetch Qdrant collection info: {}", qdrantEx.getMessage());
                stats.put("qdrant_info_error", qdrantEx.getMessage());
            }

            // Check for mismatch
            Integer qdrantCount = (Integer) stats.get("qdrant_course_embeddings_count");
            if (dbCount != null && qdrantCount != null && !dbCount.equals(qdrantCount)) {
                stats.put("warning", String.format(
                    "MISMATCH DETECTED! Database has %d published courses but Qdrant has %d embeddings. " +
                    "Please run reindex-courses to sync.", dbCount, qdrantCount));
            }

        } catch (Exception e) {
            log.error("❌ Failed to get Qdrant stats", e);
            stats.put("error", e.getMessage());
        }

        return stats;
    }
}
