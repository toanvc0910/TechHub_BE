package com.techhub.app.userservice.service.impl;

import com.techhub.app.userservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    @Async
    public void sendOTPEmail(String email, String otpCode, String purpose) {
        if ("dev".equals(activeProfile)) {
            log.info("DEV MODE: Skipping OTP email to: {} (Purpose: {}, OTP: {})", email, purpose, otpCode);
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            // Tạo context cho Thymeleaf
            Context context = new Context();
            context.setVariable("otpCode", otpCode);
            context.setVariable("purpose", purpose);

            // Render template
            String htmlContent = templateEngine.process("email/otp-email", context);

            helper.setTo(email);
            helper.setSubject("🔐 TechHub - Mã xác thực OTP");
            helper.setText(htmlContent, true);
            helper.setFrom(fromEmail);

            mailSender.send(mimeMessage);
            log.info("OTP HTML email sent successfully to: {} for purpose: {}", email, purpose);
        } catch (MessagingException e) {
            log.warn("Failed to send HTML OTP email to: {} - Error: {}", email, e.getMessage());
            // Fallback to simple text email
            sendSimpleOTPEmail(email, otpCode, purpose);
        } catch (Exception e) {
            log.warn("Failed to process OTP email template for: {} - Error: {}", email, e.getMessage());
            sendSimpleOTPEmail(email, otpCode, purpose);
        }
    }

    private void sendSimpleOTPEmail(String email, String otpCode, String purpose) {
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
            message.setFrom(fromEmail);

            mailSender.send(message);
            log.info("Fallback: OTP email sent successfully to: {} for purpose: {}", email, purpose);
        } catch (Exception e) {
            log.warn("Fallback: Failed to send OTP email to: {} for purpose: {} - Error: {}", email, purpose, e.getMessage());
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
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            // Tạo context cho Thymeleaf
            Context context = new Context();
            context.setVariable("username", username);

            // Render template
            String htmlContent = templateEngine.process("email/welcome-email", context);

            helper.setTo(email);
            helper.setSubject("🎉 Chào mừng bạn đến với TechHub!");
            helper.setText(htmlContent, true);
            helper.setFrom(fromEmail);

            mailSender.send(mimeMessage);
            log.info("Welcome HTML email sent successfully to: {}", email);
        } catch (MessagingException e) {
            log.warn("Failed to send HTML welcome email to: {} - Error: {}", email, e.getMessage());
            // Fallback to simple text email
            sendSimpleWelcomeEmail(email, username);
        } catch (Exception e) {
            log.warn("Failed to process welcome email template for: {} - Error: {}", email, e.getMessage());
            sendSimpleWelcomeEmail(email, username);
        }
    }

    private void sendSimpleWelcomeEmail(String email, String username) {
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
            message.setFrom(fromEmail);

            mailSender.send(message);
            log.info("Fallback: Welcome email sent successfully to: {}", email);
        } catch (Exception e) {
            log.warn("Fallback: Failed to send welcome email to: {} - Error: {}", email, e.getMessage());
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
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            // Tạo context cho Thymeleaf
            Context context = new Context();
            context.setVariable("otpCode", otpCode);

            // Render template
            String htmlContent = templateEngine.process("email/password-reset-email", context);

            helper.setTo(email);
            helper.setSubject("🔒 TechHub - Yêu cầu đặt lại mật khẩu");
            helper.setText(htmlContent, true);
            helper.setFrom(fromEmail);

            mailSender.send(mimeMessage);
            log.info("Password reset HTML email sent successfully to: {}", email);
        } catch (MessagingException e) {
            log.warn("Failed to send HTML password reset email to: {} - Error: {}", email, e.getMessage());
            sendSimplePasswordResetEmail(email, otpCode);
        } catch (Exception e) {
            log.warn("Failed to process password reset email template for: {} - Error: {}", email, e.getMessage());
            sendSimplePasswordResetEmail(email, otpCode);
        }
    }

    private void sendSimplePasswordResetEmail(String email, String otpCode) {
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
            message.setFrom(fromEmail);

            mailSender.send(message);
            log.info("Fallback: Password reset email sent successfully to: {}", email);
        } catch (Exception e) {
            log.warn("Fallback: Failed to send password reset email to: {} - Error: {}", email, e.getMessage());
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
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            // Tạo context cho Thymeleaf
            Context context = new Context();
            context.setVariable("username", username);

            // Render template
            String htmlContent = templateEngine.process("email/activation-email", context);

            helper.setTo(email);
            helper.setSubject("✅ TechHub - Tài khoản được kích hoạt!");
            helper.setText(htmlContent, true);
            helper.setFrom(fromEmail);

            mailSender.send(mimeMessage);
            log.info("Account activation HTML email sent successfully to: {}", email);
        } catch (MessagingException e) {
            log.warn("Failed to send HTML activation email to: {} - Error: {}", email, e.getMessage());
            sendSimpleActivationEmail(email, username);
        } catch (Exception e) {
            log.warn("Failed to process activation email template for: {} - Error: {}", email, e.getMessage());
            sendSimpleActivationEmail(email, username);
        }
    }

    private void sendSimpleActivationEmail(String email, String username) {
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
            message.setFrom(fromEmail);

            mailSender.send(message);
            log.info("Fallback: Account activation email sent successfully to: {}", email);
        } catch (Exception e) {
            log.warn("Fallback: Failed to send activation email to: {} - Error: {}", email, e.getMessage());
        }
    }
}
