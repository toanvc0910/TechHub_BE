package com.techhub.app.commonservice.kafka.publisher;

import com.techhub.app.commonservice.kafka.KafkaTopics;
import com.techhub.app.commonservice.kafka.event.notification.NotificationCommand;
import com.techhub.app.commonservice.kafka.event.notification.NotificationRecipient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationCommandPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.notification:" + KafkaTopics.NOTIFICATION_COMMAND_TOPIC + "}")
    private String notificationTopic;

    public void publish(NotificationCommand command) {
        NotificationCommand enrichedCommand = enrich(command);
        String key = resolveKey(enrichedCommand);
        kafkaTemplate.send(notificationTopic, key, enrichedCommand);
        log.info("Published notification command {} to topic {}", enrichedCommand.getCommandId(), notificationTopic);
    }

    private NotificationCommand enrich(NotificationCommand command) {
        if (command.getCommandId() == null) {
            command.setCommandId(UUID.randomUUID());
        }
        if (command.getCreatedAt() == null) {
            command.setCreatedAt(Instant.now());
        }
        return command;
    }

    private String resolveKey(NotificationCommand command) {
        if (!CollectionUtils.isEmpty(command.getRecipients())) {
            return command.getRecipients().stream()
                    .map(NotificationRecipient::getUserId)
                    .filter(id -> id != null && id.toString().trim().length() > 0)
                    .map(UUID::toString)
                    .findFirst()
                    .orElse(command.getCommandId().toString());
        }
        return command.getCommandId().toString();
    }
}
