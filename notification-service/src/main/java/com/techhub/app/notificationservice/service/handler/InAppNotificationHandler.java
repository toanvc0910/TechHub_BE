package com.techhub.app.notificationservice.service.handler;

import com.techhub.app.commonservice.kafka.event.notification.NotificationCommand;
import com.techhub.app.commonservice.kafka.event.notification.NotificationDeliveryMethod;
import com.techhub.app.commonservice.kafka.event.notification.NotificationRecipient;
import com.techhub.app.notificationservice.entity.Notification;
import com.techhub.app.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InAppNotificationHandler {

    private static final String DEFAULT_MESSAGE = "You have a new notification";

    private final NotificationService notificationService;

    public void handle(NotificationCommand command) {
        if (command == null) {
            log.warn("Skipping null notification command for in-app channel");
            return;
        }
        if (!containsInAppDelivery(command)) {
            log.debug("Command {} does not request IN_APP delivery. Ignored by in-app handler", command.getCommandId());
            return;
        }
        if (CollectionUtils.isEmpty(command.getRecipients())) {
            log.warn("Notification command {} skipped due to missing recipients", command.getCommandId());
            return;
        }

        command.getRecipients().forEach(recipient -> processRecipient(command, recipient));
    }

    private void processRecipient(NotificationCommand command, NotificationRecipient recipient) {
        UUID userId = recipient != null ? recipient.getUserId() : null;
        if (userId == null) {
            log.debug("Skip in-app notification for command {} - missing user id on recipient", command.getCommandId());
            return;
        }

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(command.getType());
        notification.setTitle(command.getTitle());
        notification.setMessage(resolveMessage(command));
        notification.setSentAt(OffsetDateTime.now());
        notification.setMetadata(mergeMetadata(command.getMetadata(), recipient));
        notification.setDeliveryMethod(NotificationDeliveryMethod.IN_APP);

        notificationService.createNotification(notification);
        log.debug("Created in-app notification for user {} from command {}", userId, command.getCommandId());
    }

    private boolean containsInAppDelivery(NotificationCommand command) {
        return !CollectionUtils.isEmpty(command.getDeliveryMethods())
                && command.getDeliveryMethods().contains(NotificationDeliveryMethod.IN_APP);
    }

    private String resolveMessage(NotificationCommand command) {
        if (StringUtils.hasText(command.getMessage())) {
            return command.getMessage();
        }
        if (StringUtils.hasText(command.getTitle())) {
            return command.getTitle();
        }
        return DEFAULT_MESSAGE;
    }

    private Map<String, Object> mergeMetadata(Map<String, Object> metadata, NotificationRecipient recipient) {
        if (metadata == null && recipient == null) {
            return null;
        }
        java.util.Map<String, Object> merged = new java.util.HashMap<>();
        if (metadata != null) {
            merged.putAll(metadata);
        }
        if (recipient != null) {
            if (StringUtils.hasText(recipient.getUsername())) {
                merged.putIfAbsent("username", recipient.getUsername());
            }
            if (StringUtils.hasText(recipient.getEmail())) {
                merged.putIfAbsent("email", recipient.getEmail());
            }
            if (recipient.getPreferences() != null) {
                merged.putIfAbsent("preferences", recipient.getPreferences());
            }
        }
        merged.values().removeIf(Objects::isNull);
        return merged.isEmpty() ? null : merged;
    }
}
