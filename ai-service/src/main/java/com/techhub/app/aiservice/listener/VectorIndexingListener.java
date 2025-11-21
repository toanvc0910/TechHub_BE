package com.techhub.app.aiservice.listener;

import com.techhub.app.commonservice.kafka.event.CourseEventPayload;
import com.techhub.app.aiservice.service.VectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Kafka listener to automatically index content into Qdrant when changes occur
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VectorIndexingListener {

    private final VectorService vectorService;

    /**
     * Listen to course events from Kafka and index to Qdrant
     */
    @KafkaListener(topics = "${kafka.topics.course-events:course-events}", groupId = "ai-service-indexing", containerFactory = "courseEventKafkaListenerContainerFactory")
    public void handleCourseEvent(CourseEventPayload event) {
        log.info("📥 Received CourseEvent from Kafka: {} for course {}", event.getEventType(), event.getCourseId());

        try {
            UUID courseId = UUID.fromString(event.getCourseId());

            switch (event.getEventType()) {
                case "CREATED":
                case "UPDATED":
                case "PUBLISHED":
                    // Index or update course in Qdrant
                    Map<String, Object> metadata = buildCourseMetadata(event);
                    vectorService.indexCourse(
                            courseId,
                            event.getTitle(),
                            event.getDescription(),
                            event.getObjectives(),
                            event.getRequirements(),
                            metadata);
                    break;

                case "DELETED":
                    // Remove course from Qdrant
                    vectorService.deleteCourse(courseId);
                    break;

                default:
                    log.warn("Unknown event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("❌ Failed to process CourseEvent for course {}", event.getCourseId(), e);
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
}
