package com.techhub.app.userservice.service.impl;

import com.techhub.app.commonservice.kafka.event.notification.NotificationCommand;
import com.techhub.app.commonservice.kafka.event.notification.NotificationDeliveryMethod;
import com.techhub.app.commonservice.kafka.event.notification.NotificationRecipient;
import com.techhub.app.commonservice.kafka.event.notification.NotificationType;
import com.techhub.app.commonservice.kafka.publisher.NotificationCommandPublisher;
import com.techhub.app.userservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final NotificationCommandPublisher notificationCommandPublisher;

    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;

    @Override
    public void sendOTPEmail(String email, String otpCode, String purpose) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("otpCode", otpCode);
        variables.put("purpose", purpose);

        publish(NotificationType.ACCOUNT,
                email,
                "TechHub - Verification Code",
                "otp-verification",
                variables);
    }

    @Override
    public void sendWelcomeEmail(String email, String username) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", username);

        publish(NotificationType.ACCOUNT,
                email,
                "Welcome to TechHub",
                "welcome-email",
                variables);
    }

    @Override
    public void sendPasswordResetEmail(String email, String otpCode) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("otpCode", otpCode);

        publish(NotificationType.ACCOUNT,
                email,
                "TechHub - Password Reset",
                "password-reset",
                variables);
    }

    @Override
    public void sendAccountActivationEmail(String email, String username) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", username);

        publish(NotificationType.ACCOUNT,
                email,
                "TechHub - Account Activated",
                "account-activation",
                variables);
    }

    private void publish(NotificationType type,
                         String recipient,
                         String subject,
                         String templateCode,
                         Map<String, Object> variables) {
        if (!emailEnabled) {
            log.info("Email delivery disabled. Skipping event for {} using template {}", recipient, templateCode);
            return;
        }

        NotificationRecipient notificationRecipient = NotificationRecipient.builder()
                .email(recipient)
                .build();

        NotificationCommand command = NotificationCommand.builder()
                .type(type)
                .title(subject)
                .message(subject)
                .templateCode(templateCode)
                .templateVariables(variables)
                .deliveryMethods(EnumSet.of(NotificationDeliveryMethod.EMAIL))
                .recipients(List.of(notificationRecipient))
                .metadata(Map.of(
                        "source", "user-service",
                        "templateCode", templateCode
                ))
                .build();

        notificationCommandPublisher.publish(command);
    }
}
