package com.techhub.app.commonservice.kafka;

public final class KafkaTopics {

    public static final String EMAIL_TOPIC = "email-notifications";
    public static final String NOTIFICATION_COMMAND_TOPIC = "notification-commands";
    public static final String PUSH_NOTIFICATION_TOPIC = "push-notifications";

    private KafkaTopics() {
        // Utility class
    }
}
