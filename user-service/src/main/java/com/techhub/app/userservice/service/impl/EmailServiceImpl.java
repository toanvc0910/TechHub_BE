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
import java.util.UUID;

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

                // OTP emails are sensitive - email only, no in-app storage
                publishEmailOnly(NotificationType.ACCOUNT,
                                email,
                                "TechHub - Verification Code",
                                "otp-verification",
                                variables);
        }

        @Override
        public void sendWelcomeEmail(UUID userId, String email, String username) {
                Map<String, Object> variables = new HashMap<>();
                variables.put("username", username);

                // Welcome notification - both email and in-app
                publishWithInApp(NotificationType.ACCOUNT,
                                userId,
                                email,
                                username,
                                "Welcome to TechHub",
                                "Welcome to TechHub! Your account has been created successfully. Start exploring courses and begin your learning journey.",
                                "welcome-email",
                                variables);
        }

        @Override
        public void sendPasswordResetEmail(String email, String otpCode) {
                Map<String, Object> variables = new HashMap<>();
                variables.put("otpCode", otpCode);

                // Password reset OTP - email only for security
                publishEmailOnly(NotificationType.ACCOUNT,
                                email,
                                "TechHub - Password Reset",
                                "password-reset",
                                variables);
        }

        @Override
        public void sendAccountActivationEmail(UUID userId, String email, String username) {
                Map<String, Object> variables = new HashMap<>();
                variables.put("username", username);

                // Account activation - both email and in-app
                publishWithInApp(NotificationType.ACCOUNT,
                                userId,
                                email,
                                username,
                                "Account Verified Successfully",
                                "Your TechHub account has been verified successfully. You now have full access to all features.",
                                "account-activation",
                                variables);
        }

        @Override
        public void sendPasswordChangedNotification(UUID userId, String email, String username) {
                Map<String, Object> variables = new HashMap<>();
                variables.put("username", username);

                // Password changed - both email and in-app for security awareness
                publishWithInApp(NotificationType.ACCOUNT,
                                userId,
                                email,
                                username,
                                "Password Changed Successfully",
                                "Your password has been changed successfully. If you didn't make this change, please contact support immediately.",
                                "password-changed",
                                variables);
        }

        @Override
        public void sendForgotPasswordNotification(UUID userId, String email, String username) {
                Map<String, Object> variables = new HashMap<>();
                variables.put("username", username);

                // Forgot password initiated - in-app only for security awareness
                publishInAppOnly(NotificationType.ACCOUNT,
                                userId,
                                email,
                                username,
                                "Password Reset Requested",
                                "A password reset was requested for your account. If you didn't request this, please secure your account.",
                                "forgot-password-initiated",
                                variables);
        }

        @Override
        public void sendPasswordResetSuccessNotification(UUID userId, String email, String username) {
                Map<String, Object> variables = new HashMap<>();
                variables.put("username", username);

                // Password reset success - both email and in-app
                publishWithInApp(NotificationType.ACCOUNT,
                                userId,
                                email,
                                username,
                                "Password Reset Successfully",
                                "Your password has been reset successfully. You can now login with your new password.",
                                "password-reset-success",
                                variables);
        }

        /**
         * Publish email-only notification (no in-app storage)
         */
        private void publishEmailOnly(NotificationType type,
                        String email,
                        String subject,
                        String templateCode,
                        Map<String, Object> variables) {
                if (!emailEnabled) {
                        log.info("Email delivery disabled. Skipping email for {} using template {}", email,
                                        templateCode);
                        return;
                }

                NotificationRecipient recipient = NotificationRecipient.builder()
                                .email(email)
                                .build();

                NotificationCommand command = NotificationCommand.builder()
                                .type(type)
                                .title(subject)
                                .message(subject)
                                .templateCode(templateCode)
                                .templateVariables(variables)
                                .deliveryMethods(EnumSet.of(NotificationDeliveryMethod.EMAIL))
                                .recipients(List.of(recipient))
                                .metadata(Map.of(
                                                "source", "user-service",
                                                "templateCode", templateCode))
                                .build();

                notificationCommandPublisher.publish(command);
                log.info("📧 Email-only notification sent to {} with template {}", email, templateCode);
        }

        /**
         * Publish notification with both EMAIL and IN_APP
         */
        private void publishWithInApp(NotificationType type,
                        UUID userId,
                        String email,
                        String username,
                        String title,
                        String message,
                        String templateCode,
                        Map<String, Object> variables) {
                NotificationRecipient recipient = NotificationRecipient.builder()
                                .userId(userId)
                                .email(email)
                                .username(username)
                                .build();

                EnumSet<NotificationDeliveryMethod> methods = emailEnabled
                                ? EnumSet.of(NotificationDeliveryMethod.EMAIL, NotificationDeliveryMethod.IN_APP)
                                : EnumSet.of(NotificationDeliveryMethod.IN_APP);

                NotificationCommand command = NotificationCommand.builder()
                                .type(type)
                                .title(title)
                                .message(message)
                                .templateCode(templateCode)
                                .templateVariables(variables)
                                .deliveryMethods(methods)
                                .recipients(List.of(recipient))
                                .metadata(Map.of(
                                                "source", "user-service",
                                                "templateCode", templateCode))
                                .build();

                notificationCommandPublisher.publish(command);
                log.info("📧💾 Email + IN_APP notification sent to userId={}, email={} with template {}",
                                userId, email, templateCode);
        }

        /**
         * Publish IN_APP only notification (no email)
         */
        private void publishInAppOnly(NotificationType type,
                        UUID userId,
                        String email,
                        String username,
                        String title,
                        String message,
                        String templateCode,
                        Map<String, Object> variables) {
                if (userId == null) {
                        log.warn("Cannot send IN_APP notification without userId for template {}", templateCode);
                        return;
                }

                NotificationRecipient recipient = NotificationRecipient.builder()
                                .userId(userId)
                                .email(email)
                                .username(username)
                                .build();

                NotificationCommand command = NotificationCommand.builder()
                                .type(type)
                                .title(title)
                                .message(message)
                                .templateCode(templateCode)
                                .templateVariables(variables)
                                .deliveryMethods(EnumSet.of(NotificationDeliveryMethod.IN_APP))
                                .recipients(List.of(recipient))
                                .metadata(Map.of(
                                                "source", "user-service",
                                                "templateCode", templateCode))
                                .build();

                notificationCommandPublisher.publish(command);
                log.info("💾 IN_APP-only notification sent to userId={} with template {}", userId, templateCode);
        }
}
