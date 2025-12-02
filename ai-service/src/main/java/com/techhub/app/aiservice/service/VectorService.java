package com.techhub.app.aiservice.service;

import com.techhub.app.aiservice.client.QdrantClient;
import com.techhub.app.aiservice.client.QdrantClient.QdrantPoint;
import com.techhub.app.aiservice.config.QdrantProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service to manage vector embeddings in Qdrant
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VectorService {

    private final QdrantClient qdrantClient;
    private final EmbeddingService embeddingService;
    private final QdrantProperties qdrantProperties;

    @PostConstruct
    public void initializeCollections() {
        int dimension = embeddingService.getEmbeddingDimension();

        // Create collections if not exist
        qdrantClient.createCollectionIfNotExists(
                qdrantProperties.getRecommendationCollection(),
                dimension);
        qdrantClient.createCollectionIfNotExists(
                qdrantProperties.getLessonCollection(),
                dimension);
        qdrantClient.createCollectionIfNotExists(
                qdrantProperties.getProfileCollection(),
                dimension);

        log.info("✅ Qdrant collections initialized");
    }

    /**
     * Index a course into Qdrant for semantic search
     */
    public void indexCourse(UUID courseId, String title, String description,
            String objectives, String requirements, Map<String, Object> metadata) {
        try {
            // Build text representation
            String text = embeddingService.buildCourseText(title, description, objectives, requirements);

            // Generate embedding
            List<Double> embedding = embeddingService.generateEmbedding(text);

            if (embedding.isEmpty()) {
                log.warn("⚠️ Skipping course indexing due to empty embedding: {}", courseId);
                return;
            }

            // Prepare payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("course_id", courseId.toString());
            payload.put("title", title);
            payload.put("description", description);
            payload.put("objectives", objectives);
            payload.put("requirements", requirements);
            if (metadata != null) {
                payload.putAll(metadata);
            }

            // Upsert to Qdrant
            QdrantPoint point = new QdrantPoint(courseId.toString(), embedding, payload);
            qdrantClient.upsertPoints(
                    qdrantProperties.getRecommendationCollection(),
                    Collections.singletonList(point));

            log.info("✅ Indexed course: {} - {}", courseId, title);
        } catch (Exception e) {
            log.error("❌ Failed to index course: {}", courseId, e);
        }
    }

    /**
     * Delete course from Qdrant
     */
    public void deleteCourse(UUID courseId) {
        try {
            qdrantClient.deletePoint(
                    qdrantProperties.getRecommendationCollection(),
                    courseId.toString());
            log.info("🗑️ Deleted course from index: {}", courseId);
        } catch (Exception e) {
            log.error("❌ Failed to delete course: {}", courseId, e);
        }
    }

    /**
     * Index user profile for personalized recommendations
     */
    public void indexUserProfile(UUID userId, String skills, String interests, String completedCourses) {
        try {
            String text = embeddingService.buildUserProfileText(
                    userId.toString(), skills, interests, completedCourses);

            List<Double> embedding = embeddingService.generateEmbedding(text);

            if (embedding.isEmpty()) {
                log.warn("⚠️ Skipping user profile indexing due to empty embedding: {}", userId);
                return;
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("user_id", userId.toString());
            payload.put("skills", skills);
            payload.put("interests", interests);
            payload.put("completed_courses", completedCourses);

            QdrantPoint point = new QdrantPoint(userId.toString(), embedding, payload);
            qdrantClient.upsertPoints(
                    qdrantProperties.getProfileCollection(),
                    Collections.singletonList(point));

            log.info("✅ Indexed user profile: {}", userId);
        } catch (Exception e) {
            log.error("❌ Failed to index user profile: {}", userId, e);
        }
    }

    /**
     * Search for similar courses based on text query
     */
    public List<Map<String, Object>> searchCourses(String query, int limit) {
        try {
            // Generate embedding for query
            List<Double> queryEmbedding = embeddingService.generateEmbedding(query);

            if (queryEmbedding.isEmpty()) {
                log.warn("⚠️ Cannot search with empty embedding");
                return Collections.emptyList();
            }

            // Search in Qdrant
            List<Map<String, Object>> results = qdrantClient.searchSimilar(
                    qdrantProperties.getRecommendationCollection(),
                    queryEmbedding,
                    limit);

            log.info("🔍 Found {} similar courses for query: {}", results.size(), query);
            return results;
        } catch (Exception e) {
            log.error("❌ Failed to search courses", e);
            return Collections.emptyList();
        }
    }

    /**
     * Get personalized recommendations based on user profile
     */
    public List<Map<String, Object>> getRecommendationsForUser(UUID userId, int limit) {
        try {
            // Get user profile embedding
            List<Map<String, Object>> userProfile = qdrantClient.searchSimilar(
                    qdrantProperties.getProfileCollection(),
                    Collections.emptyList(),
                    1);

            if (userProfile.isEmpty()) {
                log.warn("⚠️ User profile not found in Qdrant: {}", userId);
                return Collections.emptyList();
            }

            // Extract user vector from profile
            // Note: This is simplified - in real implementation, you'd fetch the vector
            // directly
            log.info("🎯 Generating recommendations for user: {}", userId);

            // For now, return empty - this would need the actual vector from the user
            // profile
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("❌ Failed to get recommendations for user: {}", userId, e);
            return Collections.emptyList();
        }
    }

    /**
     * Search similar courses based on a course ID (for "similar courses" feature)
     */
    public List<Map<String, Object>> findSimilarCourses(UUID courseId, int limit) {
        try {
            // This would require fetching the course vector first, then searching
            // Simplified implementation
            log.info("🔍 Finding similar courses to: {}", courseId);
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("❌ Failed to find similar courses", e);
            return Collections.emptyList();
        }
    }

    /**
     * Batch index multiple courses - OPTIMIZED with batch embeddings
     * This significantly reduces API calls by batching multiple texts into one embedding request
     */
    public void batchIndexCourses(List<Map<String, Object>> courses) {
        if (courses == null || courses.isEmpty()) {
            return;
        }

        try {
            // Step 1: Prepare all texts for batch embedding
            List<String> texts = new ArrayList<>();
            List<Map<String, Object>> validCourses = new ArrayList<>();
            
            for (Map<String, Object> course : courses) {
                Object idObj = course.get("id");
                String courseId = idObj != null ? idObj.toString() : null;

                if (courseId == null) {
                    log.warn("⚠️ Skipping course with null ID");
                    continue;
                }

                String title = getStringValue(course.get("title"));
                String description = getStringValue(course.get("description"));
                String objectives = getStringValue(course.get("objectives"));
                String requirements = getStringValue(course.get("requirements"));

                String text = embeddingService.buildCourseText(title, description, objectives, requirements);
                texts.add(text);
                validCourses.add(course);
            }

            if (texts.isEmpty()) {
                return;
            }

            // Step 2: Generate embeddings in batch (1 API call for all!)
            log.info("📦 Generating {} embeddings in batch...", texts.size());
            List<List<Double>> embeddings = embeddingService.generateEmbeddingsBatch(texts);

            if (embeddings.size() != validCourses.size()) {
                log.error("❌ Embedding count mismatch: {} texts vs {} embeddings", 
                    validCourses.size(), embeddings.size());
                return;
            }

            // Step 3: Build points and upsert
            List<QdrantPoint> points = new ArrayList<>();
            for (int i = 0; i < validCourses.size(); i++) {
                Map<String, Object> course = validCourses.get(i);
                List<Double> embedding = embeddings.get(i);
                
                if (embedding.isEmpty()) {
                    continue;
                }

                String courseId = course.get("id").toString();
                String title = getStringValue(course.get("title"));

                Map<String, Object> coursePayload = new HashMap<>(course);
                coursePayload.put("id", courseId);

                for (Map.Entry<String, Object> entry : coursePayload.entrySet()) {
                    Object value = entry.getValue();
                    if (value != null && !isPrimitiveOrString(value)) {
                        coursePayload.put(entry.getKey(), value.toString());
                    }
                }

                points.add(new QdrantPoint(courseId, embedding, coursePayload));
                log.debug("📦 Prepared course for indexing: {} - {}", courseId, title);
            }

            if (!points.isEmpty()) {
                qdrantClient.upsertPoints(
                        qdrantProperties.getRecommendationCollection(),
                        points);
                log.info("✅ Batch indexed {} courses with {} API call(s)", points.size(), 1);
            }
        } catch (Exception e) {
            log.error("❌ Failed to batch index courses", e);
        }
    }

    /**
     * Index a lesson into Qdrant
     */
    public void indexLesson(UUID lessonId, String title, String content, String videoUrl,
            Map<String, Object> metadata) {
        try {
            // Build text representation
            String text = title + " " + (content != null ? content : "");

            // Generate embedding
            List<Double> embedding = embeddingService.generateEmbedding(text);

            if (embedding.isEmpty()) {
                log.warn("⚠️ Skipping lesson indexing due to empty embedding: {}", lessonId);
                return;
            }

            // Prepare payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("lesson_id", lessonId.toString());
            payload.put("title", title);
            payload.put("content", content);
            payload.put("video_url", videoUrl);
            if (metadata != null) {
                payload.putAll(metadata);
            }

            // Upsert to Qdrant lesson collection
            QdrantPoint point = new QdrantPoint(lessonId.toString(), embedding, payload);
            qdrantClient.upsertPoints(
                    qdrantProperties.getLessonCollection(),
                    Collections.singletonList(point));

            log.info("✅ Indexed lesson: {} - {}", lessonId, title);
        } catch (Exception e) {
            log.error("❌ Failed to index lesson: {}", lessonId, e);
        }
    }

    /**
     * Delete lesson from Qdrant
     */
    public void deleteLesson(UUID lessonId) {
        try {
            qdrantClient.deletePoint(
                    qdrantProperties.getLessonCollection(),
                    lessonId.toString());
            log.info("🗑️ Deleted lesson from index: {}", lessonId);
        } catch (Exception e) {
            log.error("❌ Failed to delete lesson: {}", lessonId, e);
        }
    }

    /**
     * Index enrollment data for recommendations
     */
    public void indexEnrollment(UUID enrollmentId, UUID userId, UUID courseId, String status, Double progress) {
        try {
            // For enrollments, we might not need embeddings if we only use them for
            // filtering
            // But if we want to find "users with similar enrollments", we need embeddings.
            // For now, let's just store it as a point in profile collection or a new one.
            // Simplified: Update user profile with this enrollment info

            // In a real system, we might append this to the user's history text and
            // re-index the user profile.
            // Here, we'll just log it as a placeholder for future "User Embedding Update"
            // logic.
            log.info(
                    "ℹ️ Enrollment indexing requested for user {} course {}. (Logic to update user profile embedding would go here)",
                    userId, courseId);

            // Example: Fetch current user profile, append course to history, re-embed.
            // For this task, we focus on Lesson/Course indexing.
        } catch (Exception e) {
            log.error("❌ Failed to index enrollment: {}", enrollmentId, e);
        }
    }

    /**
     * Get lesson payload from Qdrant
     */
    public Map<String, Object> getLesson(UUID lessonId) {
        try {
            Map<String, Object> point = qdrantClient.retrievePoint(
                    qdrantProperties.getLessonCollection(),
                    lessonId.toString());

            if (point != null && point.containsKey("payload")) {
                return (Map<String, Object>) point.get("payload");
            }
            return null;
        } catch (Exception e) {
            log.error("❌ Failed to get lesson from Qdrant: {}", lessonId, e);
            return null;
        }
    }

    /**
     * Check if object is a primitive type or String
     */
    private boolean isPrimitiveOrString(Object obj) {
        return obj instanceof String ||
                obj instanceof Number ||
                obj instanceof Boolean ||
                obj instanceof Character ||
                obj instanceof java.util.Date ||
                obj instanceof java.time.LocalDate ||
                obj instanceof java.time.LocalDateTime ||
                obj instanceof java.time.OffsetDateTime ||
                obj instanceof java.util.UUID;
    }

    /**
     * Safely extract String value from object (handles PGobject)
     */
    private String getStringValue(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof String) {
            return (String) obj;
        }
        // Handle PGobject or any other object type
        return obj.toString();
    }
}
