package com.techhub.app.userservice.service.impl;

import com.techhub.app.userservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Override
    @Async
    public void sendOTPEmail(String email, String otpCode, String purpose) {
        if ("dev".equals(activeProfile)) {
            log.info("DEV MODE: Skipping OTP email to: {} (Purpose: {}, OTP: {})", email, purpose, otpCode);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("TechHub - Verification Code");
            message.setText(String.format(
                "Hello,\n\n" +
                "Your verification code for %s is: %s\n\n" +
                "This code will expire in 15 minutes.\n\n" +
                "If you didn't request this code, please ignore this email or contact support if you have concerns.\n\n" +
                "Best regards,\n" +
                "TechHub Team",
                purpose, otpCode));
            message.setFrom("noreply@techhub.com");

            mailSender.send(message);
            log.info("OTP email sent successfully to: {} for purpose: {}", email, purpose);
        } catch (Exception e) {
            log.warn("Failed to send OTP email to: {} for purpose: {} - Error: {}", email, purpose, e.getMessage());
        }
    }

    @Override
    @Async
    public void sendWelcomeEmail(String email, String username) {
        if ("dev".equals(activeProfile)) {
            log.info("DEV MODE: Skipping email send to: {} (Welcome message for user: {})", email, username);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Welcome to TechHub!");
            message.setText(String.format(
                "Hello %s,\n\n" +
                "Welcome to TechHub! Your account has been created successfully.\n\n" +
                "You can now start exploring our courses and learning resources.\n\n" +
                "If you have any questions, feel free to contact our support team.\n\n" +
                "Happy learning!\n\n" +
                "Best regards,\n" +
                "TechHub Team",
                username));
            message.setFrom("noreply@techhub.com");

            mailSender.send(message);
            log.info("Welcome email sent successfully to: {}", email);
        } catch (Exception e) {
            log.warn("Failed to send welcome email to: {} - Error: {}", email, e.getMessage());
            // Don't throw exception - just log and continue
        }
    }

    @Override
    @Async
    public void sendPasswordResetEmail(String email, String otpCode) {
        if ("dev".equals(activeProfile)) {
            log.info("DEV MODE: Skipping password reset email to: {} (Reset code: {})", email, otpCode);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("TechHub - Password Reset Request");
            message.setText(String.format(
                "Hello,\n\n" +
                "You have requested to reset your password for your TechHub account.\n\n" +
                "Your password reset OTP code is: %s\n\n" +
                "This code will expire in 15 minutes.\n\n" +
                "If you didn't request a password reset, please ignore this email or contact support if you have concerns.\n\n" +
                "Best regards,\n" +
                "TechHub Team",
                otpCode));
            message.setFrom("noreply@techhub.com");

            mailSender.send(message);
            log.info("Password reset email sent successfully to: {}", email);
        } catch (Exception e) {
            log.warn("Failed to send password reset email to: {} - Error: {}", email, e.getMessage());
        }
    }

    @Override
    @Async
    public void sendAccountActivationEmail(String email, String username) {
        if ("dev".equals(activeProfile)) {
            log.info("DEV MODE: Skipping activation email to: {} (User: {})", email, username);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("TechHub - Account Activated");
            message.setText(String.format(
                "Hello %s,\n\n" +
                "Great news! Your TechHub account has been activated.\n\n" +
                "You can now access all features and start your learning journey.\n\n" +
                "Thank you for joining TechHub!\n\n" +
                "Best regards,\n" +
                "TechHub Team",
                username));
            message.setFrom("noreply@techhub.com");

            mailSender.send(message);
            log.info("Account activation email sent successfully to: {}", email);
        } catch (Exception e) {
            log.warn("Failed to send activation email to: {} - Error: {}", email, e.getMessage());
        }
    }
}
