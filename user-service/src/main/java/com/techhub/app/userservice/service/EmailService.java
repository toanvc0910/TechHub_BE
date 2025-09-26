package com.techhub.app.userservice.service;

public interface EmailService {

    void sendOTPEmail(String email, String otpCode, String purpose);

    void sendWelcomeEmail(String email, String username);

    void sendPasswordResetEmail(String email, String otpCode);

    void sendAccountActivationEmail(String email, String username);
}
