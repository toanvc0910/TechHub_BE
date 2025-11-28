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
        log.info("📤 [DISPATCH] ===== START DISPATCH =====");
        if (command == null || CollectionUtils.isEmpty(command.getRecipients())) {
            log.warn("📤 [DISPATCH] Skip notification command {} due to missing recipients",
                    command != null ? command.getCommandId() : null);
            return;
        }

        Set<NotificationDeliveryMethod> deliveryMethods = resolveDeliveryMethods(command);
        log.info("📤 [DISPATCH] DeliveryMethods resolved: {}", deliveryMethods);
        log.info("📤 [DISPATCH] Processing {} recipients", command.getRecipients().size());

        command.getRecipients().forEach(recipient -> {
            log.info("📤 [DISPATCH] Processing recipient: userId={}, email={}",
                    recipient.getUserId(), recipient.getEmail());
            handleRecipient(command, recipient, deliveryMethods);
        });
        log.info("📤 [DISPATCH] ===== END DISPATCH =====");
    }

    protected void handleRecipient(NotificationCommand command,
            NotificationRecipient recipient,
            Set<NotificationDeliveryMethod> deliveryMethods) {
        log.debug("📬 [HANDLE RECIPIENT] Processing deliveryMethods: {}", deliveryMethods);

        if (deliveryMethods.contains(NotificationDeliveryMethod.IN_APP)) {
            log.info("📬 [HANDLE RECIPIENT] Creating IN_APP notification");
            createInAppNotification(command, recipient);
        }
        if (deliveryMethods.contains(NotificationDeliveryMethod.EMAIL)) {
            log.info("📬 [HANDLE RECIPIENT] Sending EMAIL notification");
            notificationDeliveryService.deliverEmail(command, recipient);
        }
        if (deliveryMethods.contains(NotificationDeliveryMethod.PUSH)) {
            log.debug("📬 [HANDLE RECIPIENT] Push notification delivery not implemented yet for {}",
                    command.getCommandId());
        }
    }

    protected void createInAppNotification(NotificationCommand command, NotificationRecipient recipient) {
        log.info("💾 [IN_APP] ===== CREATING IN_APP NOTIFICATION =====");
        UUID userId = recipient != null ? recipient.getUserId() : null;
        if (userId == null) {
            log.warn("💾 [IN_APP] Skip in-app notification for command {} due to missing user id",
                    command.getCommandId());
            return;
        }

        log.info("💾 [IN_APP] Building notification for userId: {}", userId);
        Notification notification = buildNotification(command, recipient);
        notification.setUserId(userId);
        notification.setSentAt(OffsetDateTime.now());
        notification.setMetadata(mergeMetadata(command.getMetadata(), recipient));
        notification.setMessage(resolveMessage(command, recipient));

        log.info("💾 [IN_APP] Saving notification: type={}, title={}, message={}",
                notification.getType(), notification.getTitle(), notification.getMessage());

        Notification saved = notificationService.createNotification(notification);
        log.info("💾 [IN_APP] ✅ Notification saved with id: {}", saved.getId());
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
