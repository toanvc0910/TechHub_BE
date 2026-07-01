package com.techhub.app.userservice.service;

import com.techhub.app.commonservice.enums.OtpType;

import java.util.UUID;

public interface OTPService {
    String generateOTP();

    void saveOTP(UUID userId, String otpCode, OtpType type);

    boolean validateOTP(UUID userId, String otpCode, OtpType type);

    void deleteOTP(UUID userId, OtpType type);

    boolean isOTPExpired(UUID userId, OtpType type);
}
