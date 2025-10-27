package com.techhub.app.userservice.service;

import java.util.UUID;

public interface EmailService {

    void sendOTPEmail(UUID userId, String email, String username, String otpCode, String purpose);

    void sendWelcomeEmail(UUID userId, String email, String username);

    void sendPasswordResetEmail(UUID userId, String email, String username, String otpCode);

    void sendAccountActivationEmail(UUID userId, String email, String username);
}
