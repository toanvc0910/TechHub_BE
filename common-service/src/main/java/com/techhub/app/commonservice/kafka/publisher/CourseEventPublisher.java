package com.techhub.app.commonservice.kafka.publisher;

import com.techhub.app.commonservice.kafka.KafkaTopics;
import com.techhub.app.commonservice.kafka.event.CourseEventPayload;
import com.techhub.app.commonservice.kafka.event.EnrollmentEventPayload;
import com.techhub.app.commonservice.kafka.event.LearningPathEventPayload;
import com.techhub.app.commonservice.kafka.event.LessonEventPayload;
import com.techhub.app.commonservice.kafka.event.RatingEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CourseEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.course-events:" + KafkaTopics.COURSE_EVENTS_TOPIC + "}")
    private String courseEventsTopic;

    @Value("${kafka.topics.lesson-events:lesson-events}")
    private String lessonEventsTopic;

    @Value("${kafka.topics.enrollment-events:enrollment-events}")
    private String enrollmentEventsTopic;

    @Value("${kafka.topics.rating-events:" + KafkaTopics.RATING_EVENTS_TOPIC + "}")
    private String ratingEventsTopic;

    @Value("${kafka.topics.learning-path-events:" + KafkaTopics.LEARNING_PATH_EVENTS_TOPIC + "}")
    private String learningPathEventsTopic;

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
            // Use separate topic for lesson events
            kafkaTemplate.send(lessonEventsTopic, event.getCourseId(), event);
            log.info("📤 Published LessonEvent: {} for lesson {}", event.getEventType(), event.getLessonId());
        } catch (Exception e) {
            log.error("❌ Failed to publish LessonEvent for lesson {}", event.getLessonId(), e);
        }
    }

    public void publishEnrollmentEvent(EnrollmentEventPayload event) {
        try {
            kafkaTemplate.send(enrollmentEventsTopic, event.getUserId(), event);
            log.info("📤 Published EnrollmentEvent: {} for user {} course {}", event.getEventType(), event.getUserId(), event.getCourseId());
        } catch (Exception e) {
            log.error("❌ Failed to publish EnrollmentEvent for enrollment {}", event.getEnrollmentId(), e);
        }
    }

    @Async
    public void publishRatingEvent(RatingEventPayload event) {
        try {
            kafkaTemplate.send(ratingEventsTopic, event.getCourseId(), event);
            log.info("📤 Published RatingEvent: {} for user {} course {}", event.getEventType(), event.getUserId(), event.getCourseId());
        } catch (Exception e) {
            log.error("❌ Failed to publish RatingEvent for course {}", event.getCourseId(), e);
        }
    }

    public void publishLearningPathEvent(LearningPathEventPayload event) {
        try {
            kafkaTemplate.send(learningPathEventsTopic, event.getPathId(), event);
            log.info("📤 Published LearningPathEvent: {} for path {}", event.getEventType(), event.getPathId());
        } catch (Exception e) {
            log.error("❌ Failed to publish LearningPathEvent for path {}", event.getPathId(), e);
        }
    }
}
