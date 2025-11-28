package com.techhub.app.commonservice.kafka.publisher;

import com.techhub.app.commonservice.kafka.KafkaTopics;
import com.techhub.app.commonservice.kafka.event.PermissionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Publisher for permission update events
 * Used to invalidate permission cache across services
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PermissionEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publish event when a user's permissions are updated
     * 
     * @param userId User whose permissions changed
     */
    public void publishUserPermissionUpdated(UUID userId) {
        PermissionEvent event = PermissionEvent.builder()
                .eventType(PermissionEvent.PermissionEventType.USER_PERMISSION_UPDATED)
                .userId(userId)
                .timestamp(Instant.now())
                .build();

        kafkaTemplate.send(KafkaTopics.PERMISSION_UPDATED_TOPIC, userId.toString(), event);
        log.info("Published USER_PERMISSION_UPDATED event for user: {}", userId);
    }

    /**
     * Publish event when a role's permissions are updated
     * 
     * @param roleId   Role ID
     * @param roleName Role name
     */
    public void publishRolePermissionUpdated(UUID roleId, String roleName) {
        PermissionEvent event = PermissionEvent.builder()
                .eventType(PermissionEvent.PermissionEventType.ROLE_PERMISSION_UPDATED)
                .roleId(roleId)
                .roleName(roleName)
                .timestamp(Instant.now())
                .build();

        kafkaTemplate.send(KafkaTopics.PERMISSION_UPDATED_TOPIC, roleId.toString(), event);
        log.info("Published ROLE_PERMISSION_UPDATED event for role: {} ({})", roleName, roleId);
    }

    /**
     * Publish event when a permission definition is updated
     * 
     * @param permissionId Permission ID
     * @param resource     Resource path
     * @param action       Action
     */
    public void publishPermissionUpdated(UUID permissionId, String resource, String action) {
        PermissionEvent event = PermissionEvent.builder()
                .eventType(PermissionEvent.PermissionEventType.PERMISSION_UPDATED)
                .permissionId(permissionId)
                .resource(resource)
                .action(action)
                .timestamp(Instant.now())
                .build();

        kafkaTemplate.send(KafkaTopics.PERMISSION_UPDATED_TOPIC, permissionId.toString(), event);
        log.info("Published PERMISSION_UPDATED event for permission: {} on {} {}", permissionId, action, resource);
    }

    /**
     * Publish generic permission event
     * 
     * @param event Permission event
     */
    public void publish(PermissionEvent event) {
        if (event.getTimestamp() == null) {
            event.setTimestamp(Instant.now());
        }

        String key = resolveKey(event);
        kafkaTemplate.send(KafkaTopics.PERMISSION_UPDATED_TOPIC, key, event);
        log.info("Published {} event with key: {}", event.getEventType(), key);
    }

    private String resolveKey(PermissionEvent event) {
        if (event.getUserId() != null) {
            return event.getUserId().toString();
        }
        if (event.getRoleId() != null) {
            return event.getRoleId().toString();
        }
        if (event.getPermissionId() != null) {
            return event.getPermissionId().toString();
        }
        return "permission-update";
    }
}
