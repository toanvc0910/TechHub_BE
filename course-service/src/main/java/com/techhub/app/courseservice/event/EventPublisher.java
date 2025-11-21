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
        log.debug("📤 EnrollmentEvent (not yet implemented): {} for user {}", event.getEventType(), event.getUserId());
    }

    public void publishLessonEvent(LessonEvent event) {
        log.debug("📤 LessonEvent (not yet implemented): {} for lesson {}", event.getEventType(), event.getLessonId());
    }
}
