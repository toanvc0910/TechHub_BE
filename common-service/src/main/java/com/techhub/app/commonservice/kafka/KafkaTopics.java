package com.techhub.app.commonservice.kafka;

public final class KafkaTopics {

    public static final String EMAIL_TOPIC = "email-notifications";
    public static final String NOTIFICATION_COMMAND_TOPIC = "notification-commands";

    private KafkaTopics() {
        // Utility class
    }
}
