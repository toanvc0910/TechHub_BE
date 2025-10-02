package com.techhub.app.userservice.service.impl;

import com.techhub.app.userservice.entity.OTPCode;
import com.techhub.app.userservice.enums.OTPTypeEnum;
import com.techhub.app.userservice.repository.OTPRepository;
import com.techhub.app.userservice.service.OTPService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OTPServiceImpl implements OTPService {

    private final OTPRepository otpRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generateOTP() {
        int otp = 100000 + secureRandom.nextInt(900000); // Generate 6-digit OTP
        return String.valueOf(otp);
    }

    @Override
    @Transactional
    public void saveOTP(UUID userId, String otpCode, OTPTypeEnum type) {
        // Deactivate any existing OTP for this user and type
        otpRepository.deactivateByUserIdAndType(userId, type);

        // Create new OTP
        OTPCode otpEntity = new OTPCode();
        otpEntity.setUserId(userId);
        otpEntity.setCode(otpCode);
        otpEntity.setType(type);
        otpEntity.setExpiresAt(LocalDateTime.now().plusMinutes(10)); // 10 minutes expiry

        otpRepository.save(otpEntity);
        log.info("OTP saved for user: {} type: {}", userId, type);
    }

    @Override
    @Transactional
    public boolean validateOTP(UUID userId, String otpCode, OTPTypeEnum type) {
        Optional<OTPCode> otpOptional = otpRepository.findByUserIdAndCodeAndTypeAndIsActiveTrue(userId, otpCode, type);

        if (otpOptional.isEmpty()) {
            log.warn("Invalid OTP attempted for user: {} type: {}", userId, type);
            return false;
        }

        OTPCode otp = otpOptional.get();

        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Expired OTP attempted for user: {} type: {}", userId, type);
            return false;
        }

        // Mark OTP as used (deactivate)
        otp.setIsActive(false);
        otpRepository.save(otp);

        log.info("OTP validated successfully for user: {} type: {}", userId, type);
        return true;
    }

    @Override
    @Transactional
    public void deleteOTP(UUID userId, OTPTypeEnum type) {
        otpRepository.deactivateByUserIdAndType(userId, type);
        log.info("OTP deleted for user: {} type: {}", userId, type);
    }

    @Override
    public boolean isOTPExpired(UUID userId, OTPTypeEnum type) {
        return !otpRepository.existsByUserIdAndTypeAndIsActiveTrueAndExpiresAtAfter(userId, type, LocalDateTime.now());
    }
}
