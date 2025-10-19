package com.techhub.app.userservice.service.impl;

import com.techhub.app.commonservice.kafka.event.EmailEvent;
import com.techhub.app.commonservice.kafka.publisher.EmailEventPublisher;
import com.techhub.app.userservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final EmailEventPublisher emailEventPublisher;

    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;

    @Override
    public void sendOTPEmail(String email, String otpCode, String purpose) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("otpCode", otpCode);
        variables.put("purpose", purpose);

        publish(email,
                "TechHub - Verification Code",
                "otp-verification",
                variables);
    }

    @Override
    public void sendWelcomeEmail(String email, String username) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", username);

        publish(email,
                "Welcome to TechHub",
                "welcome-email",
                variables);
    }

    @Override
    public void sendPasswordResetEmail(String email, String otpCode) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("otpCode", otpCode);

        publish(email,
                "TechHub - Password Reset",
                "password-reset",
                variables);
    }

    @Override
    public void sendAccountActivationEmail(String email, String username) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", username);

        publish(email,
                "TechHub - Account Activated",
                "account-activation",
                variables);
    }

    private void publish(String recipient,
                         String subject,
                         String templateCode,
                         Map<String, Object> variables) {
        if (!emailEnabled) {
            log.info("Email delivery disabled. Skipping event for {} using template {}", recipient, templateCode);
            return;
        }

        EmailEvent event = EmailEvent.builder()
                .recipient(recipient)
                .subject(subject)
                .templateCode(templateCode)
                .variables(variables)
                .build();
        emailEventPublisher.publish(event);
    }
}
