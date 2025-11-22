package com.techhub.app.aiservice.service;

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

    /**
     * Reindex all existing courses from PostgreSQL into Qdrant
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
     * Reindex all lessons from database to Qdrant
     */
    @Transactional(readOnly = true)
    public int reindexAllLessons() {
        log.info("🔄 Starting full lesson reindexing...");

        try {
            // Fetch all lessons with minimal necessary fields
            String sql = "SELECT l.id, l.title, l.content, l.video_url, l.course_id, l.chapter_id, l.content_type " +
                    "FROM lessons l";

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
                    metadata.put("course_id", lesson.get("course_id") != null ? lesson.get("course_id").toString() : null);
                    metadata.put("chapter_id", lesson.get("chapter_id") != null ? lesson.get("chapter_id").toString() : null);
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
            String sql = "SELECT e.id, e.user_id, e.course_id, e.status, " +
                    "(SELECT p.completion FROM progress p WHERE p.user_id = e.user_id AND p.lesson_id IN " +
                    "(SELECT l.id FROM lessons l WHERE l.course_id = e.course_id) LIMIT 1) as progress " +
                    "FROM enrollments e WHERE e.is_active = 'Y'";

            // Note: The progress calculation above is simplified. In a real scenario, 
            // we might need to aggregate progress from the progress table properly.
            // For now, we'll fetch basic enrollment info.
            
            List<Map<String, Object>> enrollments = jdbcTemplate.queryForList(sql);
            log.info("📊 Found {} enrollments to reindex", enrollments.size());

            int count = 0;
            for (Map<String, Object> enrollment : enrollments) {
                try {
                    UUID enrollmentId = (UUID) enrollment.get("id");
                    UUID userId = (UUID) enrollment.get("user_id");
                    UUID courseId = (UUID) enrollment.get("course_id");
                    String status = (String) enrollment.get("status");
                    // Double progress = ... (fetch properly if needed)
                    
                    vectorService.indexEnrollment(enrollmentId, userId, courseId, status, 0.0);
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
            "enrollments", enrollments
        );
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
