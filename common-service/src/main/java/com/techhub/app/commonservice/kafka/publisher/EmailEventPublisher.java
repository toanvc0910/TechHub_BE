package com.techhub.app.commonservice.kafka.publisher;

import com.techhub.app.commonservice.kafka.KafkaTopics;
import com.techhub.app.commonservice.kafka.event.EmailEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.email:" + KafkaTopics.EMAIL_TOPIC + "}")
    private String emailTopic;

    public void publish(EmailEvent event) {
        EmailEvent enrichedEvent = enrich(event);
        kafkaTemplate.send(emailTopic, enrichedEvent.getRecipient(), enrichedEvent);
        log.info("Published email event to topic {} for recipient {}", emailTopic, enrichedEvent.getRecipient());
    }

    private EmailEvent enrich(EmailEvent event) {
        if (event.getEventId() == null || event.getEventId().toString().isEmpty()) {
            event.setEventId(UUID.randomUUID());
        }
        if (event.getCreatedAt() == null) {
            event.setCreatedAt(Instant.now());
        }
        return event;
    }
}
