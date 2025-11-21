package com.techhub.app.commonservice.kafka;

public final class KafkaTopics {

    public static final String EMAIL_TOPIC = "email-notifications";
    public static final String NOTIFICATION_COMMAND_TOPIC = "notification-commands";

    // AI Service Topics
    public static final String COURSE_EVENTS_TOPIC = "course-events";
    public static final String ENROLLMENT_EVENTS_TOPIC = "enrollment-events";
    public static final String LESSON_EVENTS_TOPIC = "lesson-events";

    private KafkaTopics() {
        // Utility class
    }
}
