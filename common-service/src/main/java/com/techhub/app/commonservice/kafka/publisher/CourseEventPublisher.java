package com.techhub.app.commonservice.kafka.publisher;

import com.techhub.app.commonservice.kafka.KafkaTopics;
import com.techhub.app.commonservice.kafka.event.CourseEventPayload;
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
}
