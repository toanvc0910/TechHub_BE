package com.techhub.app.proxyclient.kafka;

import com.techhub.app.commonservice.kafka.KafkaTopics;
import com.techhub.app.commonservice.kafka.event.PermissionEvent;
import com.techhub.app.proxyclient.cache.PermissionCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer to listen for permission update events and invalidate cache
 * 
 * Event types:
 * - USER_PERMISSION_UPDATED: Clear cache for specific user
 * - ROLE_PERMISSION_UPDATED: Clear all cache (affects all users with that role)
 * - PERMISSION_UPDATED: Clear all cache
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PermissionCacheInvalidationConsumer {

    private final PermissionCacheService permissionCacheService;

    @KafkaListener(topics = KafkaTopics.PERMISSION_UPDATED_TOPIC, groupId = "proxy-client-permission-cache-group", containerFactory = "kafkaListenerContainerFactory")
    public void handlePermissionUpdated(PermissionEvent event, Acknowledgment acknowledgment) {
        try {
            log.info("Received permission update event: type={}, userId={}, roleId={}",
                    event.getEventType(), event.getUserId(), event.getRoleId());

            switch (event.getEventType()) {
                case USER_PERMISSION_UPDATED:
                    handleUserPermissionUpdated(event);
                    break;

                case ROLE_PERMISSION_UPDATED:
                case PERMISSION_UPDATED:
                    handleGlobalPermissionUpdated(event);
                    break;

                default:
                    log.warn("Unknown permission event type: {}", event.getEventType());
            }

            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }

        } catch (Exception e) {
            log.error("Error processing permission update event: {}", event, e);
            // Still acknowledge to avoid reprocessing
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
        }
    }

    /**
     * Clear cache for specific user when their permissions are updated
     */
    private void handleUserPermissionUpdated(PermissionEvent event) {
        try {
            if (event.getUserId() != null) {
                permissionCacheService.clearUserPermissions(event.getUserId());
                log.info("Cleared permission cache for user: {}", event.getUserId());
            }
        } catch (Exception e) {
            log.error("Error clearing user permission cache", e);
        }
    }

    /**
     * Clear all cache when role or permission definitions are updated
     */
    private void handleGlobalPermissionUpdated(PermissionEvent event) {
        try {
            permissionCacheService.clearAllPermissions();
            log.info("Cleared all permission cache due to {} update", event.getEventType());
        } catch (Exception e) {
            log.error("Error clearing all permission cache", e);
        }
    }
}
