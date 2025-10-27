package com.techhub.app.commonservice.notification;

import com.techhub.app.commonservice.kafka.event.notification.NotificationCommand;
import com.techhub.app.commonservice.kafka.event.notification.NotificationDeliveryMethod;
import com.techhub.app.commonservice.kafka.event.notification.NotificationRecipient;
import com.techhub.app.commonservice.kafka.event.notification.NotificationType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Centralised helpers to build {@link NotificationCommand NotificationCommand}s so that services
 * share the same payload structure when talking to the notification service.
 */
public final class NotificationCommandFactory {

    private static final String METADATA_CHANNEL_KEY = "channel";
    private static final String CHANNEL_EMAIL = "email";
    private static final String CHANNEL_IN_APP = "in-app";

    private NotificationCommandFactory() {
        // utility
    }

    public static NotificationCommand email(NotificationType type,
                                            String subject,
                                            String templateCode,
                                            Map<String, Object> templateVariables,
                                            NotificationRecipient recipient,
                                            Map<String, Object> metadata) {
        List<NotificationRecipient> recipients = wrapSingleRecipient(recipient);
        Map<String, Object> mergedMetadata = mergeMetadata(metadata, CHANNEL_EMAIL);

        return NotificationCommand.builder()
                .type(type)
                .title(subject)
                .message(subject)
                .templateCode(templateCode)
                .templateVariables(cleanMap(templateVariables))
                .deliveryMethods(EnumSet.of(NotificationDeliveryMethod.EMAIL))
                .recipients(recipients)
                .metadata(mergedMetadata)
                .build();
    }

    public static NotificationCommand inApp(NotificationType type,
                                            String title,
                                            String message,
                                            Iterable<NotificationRecipient> recipients,
                                            Map<String, Object> metadata) {
        List<NotificationRecipient> safeRecipients = copyRecipients(recipients);
        Map<String, Object> mergedMetadata = mergeMetadata(metadata, CHANNEL_IN_APP);

        return NotificationCommand.builder()
                .type(type)
                .title(title)
                .message(message)
                .deliveryMethods(EnumSet.of(NotificationDeliveryMethod.IN_APP))
                .recipients(safeRecipients)
                .metadata(mergedMetadata)
                .build();
    }

    public static NotificationCommand combined(NotificationType type,
                                               String title,
                                               String message,
                                               Iterable<NotificationRecipient> recipients,
                                               boolean includeEmail,
                                               Map<String, Object> metadata,
                                               Map<String, Object> templateVariables,
                                               String templateCode) {
        List<NotificationRecipient> safeRecipients = copyRecipients(recipients);
        Set<NotificationDeliveryMethod> deliveryMethods = includeEmail
                ? EnumSet.of(NotificationDeliveryMethod.IN_APP, NotificationDeliveryMethod.EMAIL)
                : EnumSet.of(NotificationDeliveryMethod.IN_APP);
        Map<String, Object> mergedMetadata = mergeMetadata(metadata, includeEmail ? null : CHANNEL_IN_APP);

        return NotificationCommand.builder()
                .type(type)
                .title(title)
                .message(message)
                .templateCode(includeEmail ? templateCode : null)
                .templateVariables(includeEmail ? cleanMap(templateVariables) : null)
                .deliveryMethods(deliveryMethods)
                .recipients(safeRecipients)
                .metadata(mergedMetadata)
                .build();
    }

    private static List<NotificationRecipient> wrapSingleRecipient(NotificationRecipient recipient) {
        return recipient == null ? List.of() : List.of(recipient);
    }

    private static Map<String, Object> mergeMetadata(Map<String, Object> metadata, String channel) {
        Map<String, Object> merged = new HashMap<>();
        if (metadata != null) {
            merged.putAll(metadata);
        }
        if (StringUtils.hasText(channel)) {
            merged.putIfAbsent(METADATA_CHANNEL_KEY, channel);
        }
        merged.values().removeIf(Objects::isNull);
        return merged.isEmpty() ? null : merged;
    }

    private static Map<String, Object> cleanMap(Map<String, Object> original) {
        if (CollectionUtils.isEmpty(original)) {
            return null;
        }
        Map<String, Object> copy = new HashMap<>(original);
        copy.values().removeIf(Objects::isNull);
        return copy.isEmpty() ? null : copy;
    }

    private static List<NotificationRecipient> copyRecipients(Iterable<NotificationRecipient> recipients) {
        if (recipients == null) {
            return List.of();
        }
        List<NotificationRecipient> list = new ArrayList<>();
        recipients.forEach(recipient -> {
            if (recipient != null) {
                list.add(recipient);
            }
        });
        return List.copyOf(list);
    }
}
