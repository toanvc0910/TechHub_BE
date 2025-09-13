package com.techhub.app.userservice.service.impl;

import com.techhub.app.userservice.entity.OTP;
import com.techhub.app.userservice.entity.User;
import com.techhub.app.userservice.enums.OtpType;
import com.techhub.app.userservice.repository.OTPRepository;
import com.techhub.app.userservice.repository.UserRepository;
import com.techhub.app.userservice.service.OTPService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OTPServiceImpl implements OTPService {

    private final OTPRepository otpRepository;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 15;

    @Override
    public String generateOTP(UUID userId, OtpType type) {
        log.info("Generating OTP for user ID: {} and type: {}", userId, type);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Invalidate existing OTPs for this user and type
        invalidateAllOTPsForUser(userId, type);

        // Generate new OTP code
        String otpCode = generateRandomOTPCode();

        // Create OTP entity
        OTP otp = new OTP();
        otp.setUser(user);
        otp.setCode(otpCode);
        otp.setType(type);
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        otp.setIsUsed(false);

        otpRepository.save(otp);

        log.info("OTP generated successfully for user ID: {}", userId);
        return otpCode;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validateOTP(String code, OtpType type) {
        return otpRepository.findByCodeAndTypeAndIsUsedFalseAndExpiresAtAfter(
            code, type, LocalDateTime.now()).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validateOTPForUser(UUID userId, String code, OtpType type) {
        return otpRepository.findByCodeAndTypeAndIsUsedFalseAndExpiresAtAfter(
            code, type, LocalDateTime.now())
            .map(otp -> otp.getUser().getId().equals(userId))
            .orElse(false);
    }

    @Override
    public void markOTPAsUsed(String code, OtpType type) {
        log.info("Marking OTP as used: {} for type: {}", code, type);

        OTP otp = otpRepository.findByCodeAndTypeAndIsUsedFalseAndExpiresAtAfter(
            code, type, LocalDateTime.now())
            .orElseThrow(() -> new RuntimeException("Invalid or expired OTP"));

        otp.setIsUsed(true);
        otpRepository.save(otp);

        log.info("OTP marked as used successfully");
    }

    @Override
    public void invalidateAllOTPsForUser(UUID userId, OtpType type) {
        log.info("Invalidating all OTPs for user ID: {} and type: {}", userId, type);
        otpRepository.markAllAsUsedByUserIdAndType(userId, type);
    }

    @Override
    public void cleanupExpiredOTPs() {
        log.info("Cleaning up expired OTPs");
        otpRepository.deleteByExpiresAtBeforeOrIsUsedTrue(LocalDateTime.now());
        log.info("Expired OTPs cleaned up successfully");
    }

    private String generateRandomOTPCode() {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(secureRandom.nextInt(10));
        }
        return otp.toString();
    }
}
