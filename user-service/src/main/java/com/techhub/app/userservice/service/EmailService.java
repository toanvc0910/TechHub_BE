package com.techhub.app.userservice.service;

import java.util.UUID;

public interface EmailService {

    /**
     * Send OTP email (email only, no in-app notification)
     */
    void sendOTPEmail(String email, String otpCode, String purpose);

    /**
     * Send welcome email with in-app notification
     */
    void sendWelcomeEmail(UUID userId, String email, String username);

    /**
     * Send password reset OTP email (email only, no in-app notification)
     */
    void sendPasswordResetEmail(String email, String otpCode);

    /**
     * Send account activation confirmation with in-app notification
     */
    void sendAccountActivationEmail(UUID userId, String email, String username);

    /**
     * Send password changed notification (in-app + email)
     */
    void sendPasswordChangedNotification(UUID userId, String email, String username);

    /**
     * Send forgot password initiated notification (in-app only - for security
     * awareness)
     */
    void sendForgotPasswordNotification(UUID userId, String email, String username);

    /**
     * Send password reset success notification (in-app + email)
     */
    void sendPasswordResetSuccessNotification(UUID userId, String email, String username);
}
