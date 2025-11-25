package com.techhub.app.aiservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledCleanupService {

    private final ChatOrchestrationService chatOrchestrationService;

    @Value("${ai.chat.cleanup.days-to-keep:30}")
    private int daysToKeep;

    /**
     * Runs every day at 2 AM to clean up old chat sessions
     * Cron: second, minute, hour, day, month, weekday
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldSessions() {
        log.info("Starting scheduled cleanup of chat sessions older than {} days", daysToKeep);
        try {
            chatOrchestrationService.cleanupOldSessions(daysToKeep);
            log.info("Successfully completed scheduled cleanup");
        } catch (Exception e) {
            log.error("Error during scheduled cleanup", e);
        }
    }
}
