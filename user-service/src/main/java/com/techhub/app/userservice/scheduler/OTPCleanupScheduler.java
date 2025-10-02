package com.techhub.app.userservice.scheduler;

import com.techhub.app.userservice.repository.OTPRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class OTPCleanupScheduler {

    private final OTPRepository otpRepository;

    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    @Transactional
    public void cleanupExpiredOTPs() {
        try {
            LocalDateTime now = LocalDateTime.now();
            otpRepository.deleteExpiredOTPs(now);
            log.debug("Cleaned up expired OTPs before: {}", now);
        } catch (Exception e) {
            log.error("Error cleaning up expired OTPs", e);
        }
    }
}
