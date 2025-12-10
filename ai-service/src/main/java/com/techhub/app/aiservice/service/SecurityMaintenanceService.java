package com.techhub.app.aiservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Scheduled tasks for security maintenance
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SecurityMaintenanceService {

    private final RateLimitingService rateLimitingService;

    /**
     * Clean up old rate limiting entries every hour
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour
    public void cleanupRateLimitCache() {
        log.info("🧹 Starting rate limit cache cleanup...");
        rateLimitingService.cleanupOldEntries();
        log.info("✅ Rate limit cache cleanup completed");
    }
}
