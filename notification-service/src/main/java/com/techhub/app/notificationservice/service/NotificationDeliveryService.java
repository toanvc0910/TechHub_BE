package com.techhub.app.notificationservice.service;

import com.techhub.app.commonservice.kafka.event.EmailEvent;
import com.techhub.app.commonservice.kafka.event.notification.NotificationCommand;
import com.techhub.app.commonservice.kafka.event.notification.NotificationRecipient;
import com.techhub.app.commonservice.kafka.event.notification.NotificationType;
import com.techhub.app.commonservice.kafka.publisher.EmailEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDeliveryService {

    private static final String DEFAULT_SUBJECT_PREFIX = "TechHub - Notification";

    private final EmailEventPublisher emailEventPublisher;

    public void deliverEmail(NotificationCommand command, NotificationRecipient recipient) {
        String email = recipient != null ? recipient.getEmail() : null;
        if (!StringUtils.hasText(email)) {
            log.debug("Skip email delivery for command {} due to missing recipient email", command.getCommandId());
            return;
        }

        EmailEvent emailEvent = EmailEvent.builder()
                .recipient(email)
                .subject(resolveSubject(command))
                .templateCode(command.getTemplateCode())
                .variables(resolveVariables(command, recipient))
                .metadata(resolveMetadata(command, recipient))
                .build();

        emailEventPublisher.publish(emailEvent);
    }

    private String resolveSubject(NotificationCommand command) {
        if (StringUtils.hasText(command.getTitle())) {
            return command.getTitle();
        }
        NotificationType type = command.getType();
        return type != null
                ? DEFAULT_SUBJECT_PREFIX + " - " + type.name()
                : DEFAULT_SUBJECT_PREFIX;
    }

    private Map<String, Object> resolveVariables(NotificationCommand command, NotificationRecipient recipient) {
        Map<String, Object> variables = new HashMap<>();
        if (command.getTemplateVariables() != null) {
            variables.putAll(command.getTemplateVariables());
        }
        Optional.ofNullable(recipient)
                .map(NotificationRecipient::getUsername)
                .ifPresent(username -> variables.putIfAbsent("username", username));
        variables.putIfAbsent("message", command.getMessage());
        return variables;
    }

    private Map<String, Object> resolveMetadata(NotificationCommand command, NotificationRecipient recipient) {
        Map<String, Object> metadata = new HashMap<>();
        if (command.getMetadata() != null) {
            metadata.putAll(command.getMetadata());
        }
        metadata.put("commandId", command.getCommandId());
        if (recipient != null && recipient.getUserId() != null) {
            metadata.put("userId", recipient.getUserId());
        }
        if (recipient != null && StringUtils.hasText(recipient.getEmail())) {
            metadata.put("email", recipient.getEmail());
        }
        return metadata;
    }
}
