package com.techhub.app.userservice.service.impl;

import com.techhub.app.userservice.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnMissingBean(JavaMailSender.class)
@Slf4j
public class NoopEmailService implements EmailService {
    @Override
    public void sendOTPEmail(String email, String otpCode, String purpose) {
        log.info("[NOOP EMAIL] sendOTPEmail to {} purpose {} code {}", email, purpose, otpCode);
    }

    @Override
    public void sendWelcomeEmail(String email, String username) {
        log.info("[NOOP EMAIL] sendWelcomeEmail to {} username {}", email, username);
    }

    @Override
    public void sendPasswordResetEmail(String email, String otpCode) {
        log.info("[NOOP EMAIL] sendPasswordResetEmail to {} code {}", email, otpCode);
    }

    @Override
    public void sendAccountActivationEmail(String email, String username) {
        log.info("[NOOP EMAIL] sendAccountActivationEmail to {} username {}", email, username);
    }
}

