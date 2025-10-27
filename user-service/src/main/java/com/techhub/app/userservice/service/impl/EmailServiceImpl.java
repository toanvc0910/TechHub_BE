package com.techhub.app.userservice.service.impl;

import com.techhub.app.commonservice.kafka.event.notification.NotificationCommand;
import com.techhub.app.commonservice.kafka.event.notification.NotificationRecipient;
import com.techhub.app.commonservice.kafka.event.notification.NotificationType;
import com.techhub.app.commonservice.notification.NotificationCommandFactory;
import com.techhub.app.commonservice.kafka.publisher.NotificationCommandPublisher;
import com.techhub.app.userservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final NotificationCommandPublisher notificationCommandPublisher;

    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;

    @Override
    public void sendOTPEmail(UUID userId, String email, String username, String otpCode, String purpose) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("otpCode", otpCode);
        variables.put("purpose", purpose);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("event", "otp");
        metadata.put("purpose", purpose);

        publish(NotificationType.ACCOUNT,
                userId,
                email,
                username,
                "TechHub - Verification Code",
                "otp-verification",
                variables,
                metadata);
    }

    @Override
    public void sendWelcomeEmail(UUID userId, String email, String username) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", username);

        Map<String, Object> metadata = Map.of("event", "welcome");

        publish(NotificationType.ACCOUNT,
                userId,
                email,
                username,
                "Welcome to TechHub",
                "welcome-email",
                variables,
                metadata);
    }

    @Override
    public void sendPasswordResetEmail(UUID userId, String email, String username, String otpCode) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("otpCode", otpCode);

        Map<String, Object> metadata = Map.of("event", "password-reset");

        publish(NotificationType.ACCOUNT,
                userId,
                email,
                username,
                "TechHub - Password Reset",
                "password-reset",
                variables,
                metadata);
    }

    @Override
    public void sendAccountActivationEmail(UUID userId, String email, String username) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", username);

        Map<String, Object> metadata = Map.of("event", "account-activation");

        publish(NotificationType.ACCOUNT,
                userId,
                email,
                username,
                "TechHub - Account Activated",
                "account-activation",
                variables,
                metadata);
    }

    private void publish(NotificationType type,
                         UUID userId,
                         String recipient,
                         String username,
                         String subject,
                         String templateCode,
                         Map<String, Object> variables,
                         Map<String, Object> metadata) {
        if (!emailEnabled) {
            log.info("Email delivery disabled. Skipping event for {} using template {}", recipient, templateCode);
            return;
        }

        NotificationRecipient notificationRecipient = NotificationRecipient.builder()
                .userId(userId)
                .email(recipient)
                .username(username)
                .build();

        Map<String, Object> enrichedMetadata = new HashMap<>();
        if (metadata != null) {
            enrichedMetadata.putAll(metadata);
        }
        enrichedMetadata.put("userId", userId);
        enrichedMetadata.put("source", "user-service");
        enrichedMetadata.put("templateCode", templateCode);

        NotificationCommand command = NotificationCommandFactory.email(
                type,
                subject,
                templateCode,
                variables,
                notificationRecipient,
                enrichedMetadata
        );
        notificationCommandPublisher.publish(command);
    }
}
