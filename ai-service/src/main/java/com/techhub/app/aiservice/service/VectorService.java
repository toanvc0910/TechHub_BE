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
     * Batch index multiple courses
     */
    public void batchIndexCourses(List<Map<String, Object>> courses) {
        if (courses == null || courses.isEmpty()) {
            return;
        }

        try {
            List<QdrantPoint> points = new ArrayList<>();

            for (Map<String, Object> course : courses) {
                String courseId = (String) course.get("id");
                String title = (String) course.get("title");
                String description = (String) course.get("description");
                String objectives = (String) course.get("objectives");
                String requirements = (String) course.get("requirements");

                String text = embeddingService.buildCourseText(title, description, objectives, requirements);
                List<Double> embedding = embeddingService.generateEmbedding(text);

                if (!embedding.isEmpty()) {
                    points.add(new QdrantPoint(courseId, embedding, course));
                }
            }

            if (!points.isEmpty()) {
                qdrantClient.upsertPoints(
                        qdrantProperties.getRecommendationCollection(),
                        points);
                log.info("✅ Batch indexed {} courses", points.size());
            }
        } catch (Exception e) {
            log.error("❌ Failed to batch index courses", e);
        }
    }
}
