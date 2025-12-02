package com.techhub.app.aiservice.listener;

import com.techhub.app.commonservice.kafka.event.CourseEventPayload;
import com.techhub.app.commonservice.kafka.event.LessonEventPayload;
import com.techhub.app.aiservice.service.VectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kafka listener to automatically index content into Qdrant when changes occur
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VectorIndexingListener {

    private final VectorService vectorService;
    
    // Deduplication cache: courseId -> last processed timestamp
    // Events within 5 seconds of each other for the same course are skipped
    private final Map<String, Long> recentlyProcessedCourses = new ConcurrentHashMap<>();
    private final Map<String, Long> recentlyProcessedLessons = new ConcurrentHashMap<>();
    private static final long DEDUP_WINDOW_MS = 5000; // 5 seconds

    /**
     * Listen to course events from Kafka and index to Qdrant
     */
    @KafkaListener(topics = "${kafka.topics.course-events:course-events}", groupId = "ai-service-indexing", containerFactory = "courseEventKafkaListenerContainerFactory")
    public void handleCourseEvent(CourseEventPayload event) {
        String courseId = event.getCourseId();
        String eventKey = courseId + ":" + event.getEventType();
        
        // Deduplication check
        Long lastProcessed = recentlyProcessedCourses.get(eventKey);
        long now = System.currentTimeMillis();
        
        if (lastProcessed != null && (now - lastProcessed) < DEDUP_WINDOW_MS) {
            log.debug("⏭️ Skipping duplicate CourseEvent: {} for course {} (processed {}ms ago)", 
                event.getEventType(), courseId, now - lastProcessed);
            return;
        }
        
        recentlyProcessedCourses.put(eventKey, now);
        // Cleanup old entries periodically
        cleanupOldEntries(recentlyProcessedCourses);
        
        log.info("📥 Received CourseEvent from Kafka: {} for course {}", event.getEventType(), courseId);

        try {
            UUID courseUuid = UUID.fromString(courseId);

            switch (event.getEventType()) {
                case "CREATED":
                case "UPDATED":
                case "PUBLISHED":
                    // Index or update course in Qdrant
                    Map<String, Object> metadata = buildCourseMetadata(event);
                    vectorService.indexCourse(
                            courseUuid,
                            event.getTitle(),
                            event.getDescription(),
                            event.getObjectives(),
                            event.getRequirements(),
                            metadata);
                    break;

                case "DELETED":
                    // Remove course from Qdrant
                    vectorService.deleteCourse(courseUuid);
                    break;

                default:
                    log.warn("Unknown event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("❌ Failed to process CourseEvent for course {}", courseId, e);
        }
    }

    /**
     * Listen to lesson events from Kafka and index to Qdrant
     * Note: Uses separate topic for lesson events
     */
    @KafkaListener(topics = "${kafka.topics.lesson-events:lesson-events}", groupId = "ai-service-lesson-indexing", containerFactory = "lessonEventKafkaListenerContainerFactory")
    public void handleLessonEvent(LessonEventPayload event) {
        // Safety check - skip if not a lesson event
        if (event == null || event.getLessonId() == null) {
            return;
        }
        
        String lessonId = event.getLessonId();
        String eventKey = lessonId + ":" + event.getEventType();
        
        // Deduplication check
        Long lastProcessed = recentlyProcessedLessons.get(eventKey);
        long now = System.currentTimeMillis();
        
        if (lastProcessed != null && (now - lastProcessed) < DEDUP_WINDOW_MS) {
            log.debug("⏭️ Skipping duplicate LessonEvent: {} for lesson {} (processed {}ms ago)", 
                event.getEventType(), lessonId, now - lastProcessed);
            return;
        }
        
        recentlyProcessedLessons.put(eventKey, now);
        cleanupOldEntries(recentlyProcessedLessons);
        
        log.info("📥 Received LessonEvent from Kafka: {} for lesson {}", event.getEventType(), lessonId);

        try {
            UUID lessonUuid = UUID.fromString(lessonId);

            switch (event.getEventType()) {
                case "CREATED":
                case "UPDATED":
                    // Index lesson
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("course_id", event.getCourseId());
                    metadata.put("chapter_id", event.getChapterId());
                    metadata.put("content_type", event.getContentType());
                    
                    vectorService.indexLesson(
                            lessonUuid,
                            event.getTitle(),
                            event.getContent(),
                            event.getVideoUrl(),
                            metadata);
                    break;

                case "DELETED":
                    // Remove lesson from Qdrant
                    vectorService.deleteLesson(lessonUuid);
                    break;
            }
        } catch (Exception e) {
            log.error("❌ Failed to process LessonEvent for lesson {}", lessonId, e);
        }
    }

    /**
     * Listen to enrollment events
     * Note: Uses separate topic for enrollment events  
     */
    @KafkaListener(topics = "${kafka.topics.enrollment-events:enrollment-events}", groupId = "ai-service-enrollment-indexing", containerFactory = "enrollmentEventKafkaListenerContainerFactory")
    public void handleEnrollmentEvent(com.techhub.app.commonservice.kafka.event.EnrollmentEventPayload event) {
        // Safety check
        if (event == null || event.getEnrollmentId() == null) {
            return;
        }
        
        log.info("📥 Received EnrollmentEvent from Kafka: {} for user {}", event.getEventType(), event.getUserId());

        try {
            vectorService.indexEnrollment(
                    UUID.fromString(event.getEnrollmentId()),
                    UUID.fromString(event.getUserId()),
                    UUID.fromString(event.getCourseId()),
                    event.getStatus(),
                    event.getProgressPercentage());
        } catch (Exception e) {
            log.error("❌ Failed to process EnrollmentEvent", e);
        }
    }

    /**
     * Build metadata map for course indexing
     */
    private Map<String, Object> buildCourseMetadata(CourseEventPayload event) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("categories", event.getCategories());
        metadata.put("tags", event.getTags());
        metadata.put("level", event.getLevel());
        metadata.put("language", event.getLanguage());
        metadata.put("instructor_id", event.getInstructorId());
        metadata.put("status", event.getStatus());
        return metadata;
    }
    
    /**
     * Cleanup old entries from deduplication cache to prevent memory leaks
     */
    private void cleanupOldEntries(Map<String, Long> cache) {
        long now = System.currentTimeMillis();
        long threshold = now - (DEDUP_WINDOW_MS * 2); // Keep entries for 2x the dedup window
        cache.entrySet().removeIf(entry -> entry.getValue() < threshold);
    }
}
