package com.techhub.app.aiservice.config;

import com.techhub.app.commonservice.kafka.event.CourseEventPayload;
import com.techhub.app.commonservice.kafka.event.LessonEventPayload;
import com.techhub.app.commonservice.kafka.event.EnrollmentEventPayload;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Value("${kafka.bootstrap-servers:}")
    private String bootstrapServers;

    /**
     * Base Kafka consumer properties
     */
    private Map<String, Object> baseConsumerConfig(String groupId) {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        config.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        config.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.techhub.app.commonservice.kafka.event");
        config.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return config;
    }

    // ==================== COURSE EVENTS ====================
    
    @Bean
    public ConsumerFactory<String, CourseEventPayload> courseEventConsumerFactory() {
        Map<String, Object> config = baseConsumerConfig("ai-service-indexing");
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, CourseEventPayload.class.getName());
        return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(),
                new JsonDeserializer<>(CourseEventPayload.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CourseEventPayload> courseEventKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, CourseEventPayload> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(courseEventConsumerFactory());
        factory.getContainerProperties()
                .setAckMode(org.springframework.kafka.listener.ContainerProperties.AckMode.BATCH);
        return factory;
    }

    // ==================== LESSON EVENTS ====================
    
    @Bean
    public ConsumerFactory<String, LessonEventPayload> lessonEventConsumerFactory() {
        Map<String, Object> config = baseConsumerConfig("ai-service-lesson-indexing");
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, LessonEventPayload.class.getName());
        return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(),
                new JsonDeserializer<>(LessonEventPayload.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, LessonEventPayload> lessonEventKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, LessonEventPayload> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(lessonEventConsumerFactory());
        factory.getContainerProperties()
                .setAckMode(org.springframework.kafka.listener.ContainerProperties.AckMode.BATCH);
        return factory;
    }

    // ==================== ENROLLMENT EVENTS ====================
    
    @Bean
    public ConsumerFactory<String, EnrollmentEventPayload> enrollmentEventConsumerFactory() {
        Map<String, Object> config = baseConsumerConfig("ai-service-enrollment-indexing");
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, EnrollmentEventPayload.class.getName());
        return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(),
                new JsonDeserializer<>(EnrollmentEventPayload.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EnrollmentEventPayload> enrollmentEventKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, EnrollmentEventPayload> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(enrollmentEventConsumerFactory());
        factory.getContainerProperties()
                .setAckMode(org.springframework.kafka.listener.ContainerProperties.AckMode.BATCH);
        return factory;
    }
}
