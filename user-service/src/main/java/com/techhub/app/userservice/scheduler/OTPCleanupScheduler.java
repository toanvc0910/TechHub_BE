package com.techhub.app.userservice.scheduler;

import com.techhub.app.userservice.service.OTPService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OTPCleanupScheduler {

    private final OTPService otpService;

    @Scheduled(fixedRate = 3600000) // Run every hour (3600000 ms)
    public void cleanupExpiredOTPs() {
        log.info("Starting OTP cleanup task");
        try {
            otpService.cleanupExpiredOTPs();
            log.info("OTP cleanup task completed successfully");
        } catch (Exception e) {
            log.error("Error during OTP cleanup task", e);
        }
    }
}
