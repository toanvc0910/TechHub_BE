package com.techhub.app.userservice.service.impl;

import com.techhub.app.userservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean(JavaMailSender.class)
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendOTPEmail(String email, String otpCode, String purpose) {
        log.info("Sending OTP email to: {} for purpose: {}", email, purpose);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("TechHub - OTP Verification");
        message.setText(String.format(
            "Your OTP code for %s is: %s\n\n" +
            "This code will expire in 15 minutes.\n" +
            "If you didn't request this, please ignore this email.\n\n" +
            "Best regards,\n" +
            "TechHub Team",
            purpose, otpCode));

        try {
            mailSender.send(message);
            log.info("OTP email sent successfully to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send OTP email to: {}", email, e);
            throw new RuntimeException("Failed to send email");
        }
    }

    @Override
    public void sendWelcomeEmail(String email, String username) {
        log.info("Sending welcome email to: {}", email);

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

        try {
            mailSender.send(message);
            log.info("Welcome email sent successfully to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", email, e);
        }
    }

    @Override
    public void sendPasswordResetEmail(String email, String otpCode) {
        log.info("Sending password reset email to: {}", email);

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

        try {
            mailSender.send(message);
            log.info("Password reset email sent successfully to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", email, e);
            throw new RuntimeException("Failed to send email");
        }
    }

    @Override
    public void sendAccountActivationEmail(String email, String username) {
        log.info("Sending account activation email to: {}", email);

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

        try {
            mailSender.send(message);
            log.info("Account activation email sent successfully to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send account activation email to: {}", email, e);
        }
    }
}
