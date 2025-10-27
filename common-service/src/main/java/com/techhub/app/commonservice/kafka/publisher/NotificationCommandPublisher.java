package com.techhub.app.commonservice.kafka.publisher;

import com.techhub.app.commonservice.kafka.KafkaTopics;
import com.techhub.app.commonservice.kafka.event.notification.NotificationCommand;
import com.techhub.app.commonservice.kafka.event.notification.NotificationDeliveryMethod;
import com.techhub.app.commonservice.kafka.event.notification.NotificationRecipient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationCommandPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.notification:" + KafkaTopics.NOTIFICATION_COMMAND_TOPIC + "}")
    private String inAppTopic;

    @Value("${kafka.topics.email:" + KafkaTopics.EMAIL_TOPIC + "}")
    private String emailTopic;

    @Value("${kafka.topics.push:" + KafkaTopics.PUSH_NOTIFICATION_TOPIC + "}")
    private String pushTopic;

    public void publish(NotificationCommand command) {
        NotificationCommand enrichedCommand = enrich(command);
        Set<NotificationDeliveryMethod> deliveryMethods = resolveDeliveryMethods(enrichedCommand);

        for (NotificationDeliveryMethod method : deliveryMethods) {
            NotificationCommand channelCommand = scopedCommand(enrichedCommand, method);
            String key = resolveKey(channelCommand);
            String topic = resolveTopic(method);
            if (topic == null) {
                log.warn("No Kafka topic configured for delivery method {}. Command {} skipped", method, channelCommand.getCommandId());
                continue;
            }
            kafkaTemplate.send(topic, key, channelCommand);
            log.info("Published notification command {} to topic {} for channel {}", channelCommand.getCommandId(), topic, method);
        }
    }

    private NotificationCommand enrich(NotificationCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Notification command must not be null");
        }

        NotificationCommand.NotificationCommandBuilder builder = command.toBuilder();
        if (command.getCommandId() == null) {
            builder.commandId(UUID.randomUUID());
        }
        if (command.getCreatedAt() == null) {
            builder.createdAt(Instant.now());
        }
        return builder.build();
    }

    private Set<NotificationDeliveryMethod> resolveDeliveryMethods(NotificationCommand command) {
        if (CollectionUtils.isEmpty(command.getDeliveryMethods())) {
            return EnumSet.of(NotificationDeliveryMethod.IN_APP);
        }
        return EnumSet.copyOf(command.getDeliveryMethods());
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

    private NotificationCommand scopedCommand(NotificationCommand command, NotificationDeliveryMethod method) {
        return command.toBuilder()
                .deliveryMethods(EnumSet.of(method))
                .metadata(augmentMetadata(command.getMetadata(), method))
                .build();
    }

    private Map<String, Object> augmentMetadata(Map<String, Object> metadata, NotificationDeliveryMethod method) {
        if (metadata == null) {
            return Map.of("channel", method.name());
        }
        if (metadata.containsKey("channel")) {
            return metadata;
        }
        Map<String, Object> copy = new java.util.HashMap<>(metadata);
        copy.put("channel", method.name());
        copy.values().removeIf(Objects::isNull);
        return copy;
    }

    private String resolveTopic(NotificationDeliveryMethod method) {
        switch (method) {
            case IN_APP:
                return inAppTopic;
            case EMAIL:
                return emailTopic;
            case PUSH:
                return pushTopic;
            default:
                return null;
        }
    }
}
