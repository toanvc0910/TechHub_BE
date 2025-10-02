package com.techhub.app.userservice.service;

import com.techhub.app.userservice.enums.OTPTypeEnum;

import java.util.UUID;

public interface OTPService {
    String generateOTP();
    void saveOTP(UUID userId, String otpCode, OTPTypeEnum type);
    boolean validateOTP(UUID userId, String otpCode, OTPTypeEnum type);
    void deleteOTP(UUID userId, OTPTypeEnum type);
    boolean isOTPExpired(UUID userId, OTPTypeEnum type);
}
