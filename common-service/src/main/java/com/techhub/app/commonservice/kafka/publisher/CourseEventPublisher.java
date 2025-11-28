package com.techhub.app.commonservice.kafka.publisher;

import com.techhub.app.commonservice.kafka.KafkaTopics;
import com.techhub.app.commonservice.kafka.event.CourseEventPayload;
import com.techhub.app.commonservice.kafka.event.EnrollmentEventPayload;
import com.techhub.app.commonservice.kafka.event.LessonEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CourseEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.course-events:" + KafkaTopics.COURSE_EVENTS_TOPIC + "}")
    private String courseEventsTopic;

    public void publishCourseEvent(CourseEventPayload event) {
        try {
            kafkaTemplate.send(courseEventsTopic, event.getCourseId(), event);
            log.info("📤 Published CourseEvent: {} for course {}", event.getEventType(), event.getCourseId());
        } catch (Exception e) {
            log.error("❌ Failed to publish CourseEvent for course {}", event.getCourseId(), e);
        }
    }

    public void publishLessonEvent(LessonEventPayload event) {
        try {
            // Use courseId as key to ensure ordering if needed, or lessonId
            kafkaTemplate.send(courseEventsTopic, event.getCourseId(), event);
            log.info("📤 Published LessonEvent: {} for lesson {}", event.getEventType(), event.getLessonId());
        } catch (Exception e) {
            log.error("❌ Failed to publish LessonEvent for lesson {}", event.getLessonId(), e);
        }
    }

    public void publishEnrollmentEvent(EnrollmentEventPayload event) {
        try {
            kafkaTemplate.send(courseEventsTopic, event.getUserId(), event); // Key by userId or courseId
            log.info("📤 Published EnrollmentEvent: {} for user {} course {}", event.getEventType(), event.getUserId(), event.getCourseId());
        } catch (Exception e) {
            log.error("❌ Failed to publish EnrollmentEvent for enrollment {}", event.getEnrollmentId(), e);
        }
    }
}
