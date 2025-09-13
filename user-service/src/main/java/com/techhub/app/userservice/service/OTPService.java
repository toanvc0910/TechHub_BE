package com.techhub.app.userservice.service;

import com.techhub.app.userservice.enums.OtpType;

import java.util.UUID;

public interface OTPService {

    String generateOTP(UUID userId, OtpType type);

    boolean validateOTP(String code, OtpType type);

    boolean validateOTPForUser(UUID userId, String code, OtpType type);

    void markOTPAsUsed(String code, OtpType type);

    void invalidateAllOTPsForUser(UUID userId, OtpType type);

    void cleanupExpiredOTPs();
}
