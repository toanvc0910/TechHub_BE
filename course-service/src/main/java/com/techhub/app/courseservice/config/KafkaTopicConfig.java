package com.techhub.app.courseservice.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration class to auto-create Kafka topics when the application starts.
 * This ensures topics exist before publishing events.
 */
@Configuration
public class KafkaTopicConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${kafka.topics.course-events:course-events}")
    private String courseEventsTopic;

    @Value("${kafka.topics.lesson-events:lesson-events}")
    private String lessonEventsTopic;

    @Value("${kafka.topics.enrollment-events:enrollment-events}")
    private String enrollmentEventsTopic;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic courseEventsTopic() {
        return TopicBuilder.name(courseEventsTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic lessonEventsTopic() {
        return TopicBuilder.name(lessonEventsTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic enrollmentEventsTopic() {
        return TopicBuilder.name(enrollmentEventsTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
