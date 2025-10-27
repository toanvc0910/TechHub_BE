package com.techhub.app.notificationservice.service;

import com.techhub.app.commonservice.kafka.event.notification.NotificationCommand;
import com.techhub.app.commonservice.kafka.event.notification.NotificationRecipient;
import com.techhub.app.commonservice.kafka.event.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDeliveryService {

    private static final String DEFAULT_SUBJECT_PREFIX = "TechHub - Notification";

    private final EmailSender emailSender;

    public void deliverEmail(NotificationCommand command, NotificationRecipient recipient) {
        String email = recipient != null ? recipient.getEmail() : null;
        if (!StringUtils.hasText(email)) {
            log.debug("Skip email delivery for command {} due to missing recipient email", command.getCommandId());
            return;
        }

        String subject = resolveSubject(command);
        Map<String, Object> variables = resolveVariables(command, recipient);
        String fallbackBody = resolvePlainMessage(command, variables);

        emailSender.send(email, subject, command.getTemplateCode(), variables, fallbackBody);
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
        if (recipient != null && StringUtils.hasText(recipient.getUsername())) {
            variables.putIfAbsent("username", recipient.getUsername());
        }
        if (recipient != null && StringUtils.hasText(recipient.getEmail())) {
            variables.putIfAbsent("email", recipient.getEmail());
        }
        variables.putIfAbsent("message", command.getMessage());
        return variables;
    }

    private String resolvePlainMessage(NotificationCommand command, Map<String, Object> variables) {
        if (StringUtils.hasText(command.getMessage())) {
            return command.getMessage();
        }
        if (variables.containsKey("otpCode")) {
            return "Your verification code is: " + variables.get("otpCode");
        }
        return resolveSubject(command);
    }
}
