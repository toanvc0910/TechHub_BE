package com.techhub.app.commonservice.kafka.publisher;

import com.techhub.app.commonservice.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Publisher for endpoint security policy change events.
 * Proxy-client listens and reloads its policy cache.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EndpointSecurityEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishPolicyUpdated() {
        Map<String, Object> event = Map.of(
                "type", "ENDPOINT_SECURITY_UPDATED",
                "timestamp", System.currentTimeMillis());
        kafkaTemplate.send(KafkaTopics.ENDPOINT_SECURITY_UPDATED_TOPIC, "policy", event);
        log.info("Published ENDPOINT_SECURITY_UPDATED event");
    }
}
