package com.techhub.app.notificationservice.processor;

import com.techhub.app.commonservice.kafka.event.notification.NotificationCommand;
import com.techhub.app.commonservice.kafka.event.notification.NotificationDeliveryMethod;
import com.techhub.app.commonservice.kafka.event.notification.NotificationRecipient;
import com.techhub.app.notificationservice.entity.Notification;
import com.techhub.app.notificationservice.service.NotificationDeliveryService;
import com.techhub.app.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
public abstract class AbstractNotificationProcessor implements NotificationProcessor {

    private final NotificationService notificationService;
    private final NotificationDeliveryService notificationDeliveryService;

    protected void dispatch(NotificationCommand command) {
        if (command == null || CollectionUtils.isEmpty(command.getRecipients())) {
            log.warn("Skip notification command {} due to missing recipients", command != null ? command.getCommandId() : null);
            return;
        }

        Set<NotificationDeliveryMethod> deliveryMethods = resolveDeliveryMethods(command);
        command.getRecipients().forEach(recipient -> handleRecipient(command, recipient, deliveryMethods));
    }

    protected void handleRecipient(NotificationCommand command,
                                   NotificationRecipient recipient,
                                   Set<NotificationDeliveryMethod> deliveryMethods) {
        if (deliveryMethods.contains(NotificationDeliveryMethod.IN_APP)) {
            createInAppNotification(command, recipient);
        }
        if (deliveryMethods.contains(NotificationDeliveryMethod.EMAIL)) {
            notificationDeliveryService.deliverEmail(command, recipient);
        }
        if (deliveryMethods.contains(NotificationDeliveryMethod.PUSH)) {
            log.debug("Push notification delivery not implemented yet for {}", command.getCommandId());
        }
    }

    protected void createInAppNotification(NotificationCommand command, NotificationRecipient recipient) {
        UUID userId = recipient != null ? recipient.getUserId() : null;
        if (userId == null) {
            log.debug("Skip in-app notification for command {} due to missing user id", command.getCommandId());
            return;
        }

        Notification notification = buildNotification(command, recipient);
        notification.setUserId(userId);
        notification.setSentAt(OffsetDateTime.now());
        notification.setMetadata(mergeMetadata(command.getMetadata(), recipient));
        notification.setMessage(resolveMessage(command, recipient));

        notificationService.createNotification(notification);
    }

    protected Notification buildNotification(NotificationCommand command, NotificationRecipient recipient) {
        Notification notification = new Notification();
        notification.setType(command.getType());
        notification.setTitle(command.getTitle());
        notification.setDeliveryMethod(NotificationDeliveryMethod.IN_APP);
        return notification;
    }

    protected String resolveMessage(NotificationCommand command, NotificationRecipient recipient) {
        return StringUtils.hasText(command.getMessage())
                ? command.getMessage()
                : buildFallbackMessage(command, recipient);
    }

    protected String buildFallbackMessage(NotificationCommand command, NotificationRecipient recipient) {
        return "You have a new notification";
    }

    protected Map<String, Object> mergeMetadata(Map<String, Object> commandMetadata, NotificationRecipient recipient) {
        if (commandMetadata == null && recipient == null) {
            return null;
        }
        Map<String, Object> metadata = new java.util.HashMap<>();
        if (commandMetadata != null) {
            metadata.putAll(commandMetadata);
        }
        if (recipient != null) {
            if (recipient.getUsername() != null) {
                metadata.putIfAbsent("username", recipient.getUsername());
            }
            if (recipient.getEmail() != null) {
                metadata.putIfAbsent("email", recipient.getEmail());
            }
            if (recipient.getPreferences() != null) {
                metadata.putIfAbsent("preferences", recipient.getPreferences());
            }
        }
        metadata.entrySet().removeIf(entry -> Objects.isNull(entry.getValue()));
        return metadata;
    }

    protected Set<NotificationDeliveryMethod> resolveDeliveryMethods(NotificationCommand command) {
        if (CollectionUtils.isEmpty(command.getDeliveryMethods())) {
            return EnumSet.of(NotificationDeliveryMethod.IN_APP);
        }
        return EnumSet.copyOf(command.getDeliveryMethods());
    }
}
