package com.techhub.app.courseservice.event;

import com.techhub.app.commonservice.kafka.event.CourseEventPayload;
import com.techhub.app.commonservice.kafka.publisher.CourseEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Publisher for Kafka events
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {

    private final CourseEventPublisher kafkaPublisher;

    public void publishCourseEvent(CourseEvent event) {
        log.debug("📤 Publishing CourseEvent via Kafka: {} for course {}", event.getEventType(), event.getCourseId());

        CourseEventPayload payload = CourseEventPayload.builder()
                .eventType(event.getEventType().name())
                .courseId(event.getCourseId() != null ? event.getCourseId().toString() : null)
                .title(event.getTitle())
                .description(event.getDescription())
                .objectives(event.getObjectives())
                .requirements(event.getRequirements())
                .categories(event.getCategories())
                .tags(event.getTags())
                .level(event.getLevel())
                .language(event.getLanguage())
                .instructorId(event.getInstructorId() != null ? event.getInstructorId().toString() : null)
                .status(event.getStatus())
                .build();

        kafkaPublisher.publishCourseEvent(payload);
    }

    // Keep these for future use
    public void publishEnrollmentEvent(EnrollmentEvent event) {
        log.debug("📤 Publishing EnrollmentEvent via Kafka: {} for user {}", event.getEventType(), event.getUserId());
        
        try {
            com.techhub.app.commonservice.kafka.event.EnrollmentEventPayload payload = com.techhub.app.commonservice.kafka.event.EnrollmentEventPayload.builder()
                    .eventType(event.getEventType().name())
                    .enrollmentId(event.getEnrollmentId() != null ? event.getEnrollmentId().toString() : null)
                    .userId(event.getUserId() != null ? event.getUserId().toString() : null)
                    .courseId(event.getCourseId() != null ? event.getCourseId().toString() : null)
                    .status(event.getStatus())
                    .progressPercentage(event.getProgressPercentage())
                    .completedLessons(event.getCompletedLessons())
                    .totalLessons(event.getTotalLessons())
                    .build();

            kafkaPublisher.publishEnrollmentEvent(payload);
        } catch (Exception e) {
            log.error("❌ Failed to publish EnrollmentEvent", e);
        }
    }

    public void publishLessonEvent(LessonEvent event) {
        log.debug("📤 Publishing LessonEvent via Kafka: {} for lesson {}", event.getEventType(), event.getLessonId());

        try {
            com.techhub.app.commonservice.kafka.event.LessonEventPayload payload = com.techhub.app.commonservice.kafka.event.LessonEventPayload.builder()
                    .eventType(event.getEventType().name())
                    .lessonId(event.getLessonId() != null ? event.getLessonId().toString() : null)
                    .courseId(event.getCourseId() != null ? event.getCourseId().toString() : null)
                    .chapterId(event.getChapterId() != null ? event.getChapterId().toString() : null)
                    .title(event.getTitle())
                    .content(event.getContent())
                    .contentType(event.getContentType())
                    .order(event.getOrder())
                    .isFree(event.getIsFree())
                    // videoUrl is not in LessonEvent yet, assuming it might be added later or not needed for now
                    .build();

            kafkaPublisher.publishLessonEvent(payload);
        } catch (Exception e) {
            log.error("❌ Failed to publish LessonEvent", e);
        }
    }
}
