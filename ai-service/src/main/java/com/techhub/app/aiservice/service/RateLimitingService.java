package com.techhub.app.aiservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for rate limiting AI requests to prevent abuse
 * Uses in-memory cache with sliding window algorithm
 */
@Service
@Slf4j
public class RateLimitingService {

    // Store request timestamps per user
    private final Map<UUID, RequestWindow> userRequestWindows = new ConcurrentHashMap<>();

    // Configuration
    private static final int MAX_REQUESTS_PER_MINUTE = 20;
    private static final int MAX_REQUESTS_PER_HOUR = 100;
    private static final Duration WINDOW_DURATION = Duration.ofMinutes(1);
    private static final Duration HOURLY_WINDOW_DURATION = Duration.ofHours(1);

    /**
     * Check if user is allowed to make a request
     * 
     * @param userId User ID
     * @return true if request is allowed, false if rate limit exceeded
     */
    public boolean isAllowed(UUID userId) {
        if (userId == null) {
            log.warn("Null userId provided for rate limiting check");
            return false;
        }

        RequestWindow window = userRequestWindows.computeIfAbsent(userId, k -> new RequestWindow());

        Instant now = Instant.now();

        // Clean up old entries
        window.cleanupOldRequests(now);

        // Check minute rate limit
        long requestsInLastMinute = window.countRequestsInWindow(now, WINDOW_DURATION);
        if (requestsInLastMinute >= MAX_REQUESTS_PER_MINUTE) {
            log.warn("User {} exceeded per-minute rate limit: {}/{}", userId, requestsInLastMinute,
                    MAX_REQUESTS_PER_MINUTE);
            return false;
        }

        // Check hourly rate limit
        long requestsInLastHour = window.countRequestsInWindow(now, HOURLY_WINDOW_DURATION);
        if (requestsInLastHour >= MAX_REQUESTS_PER_HOUR) {
            log.warn("User {} exceeded per-hour rate limit: {}/{}", userId, requestsInLastHour, MAX_REQUESTS_PER_HOUR);
            return false;
        }

        // Record this request
        window.addRequest(now);

        log.debug("Rate limit check passed for user {}: {}/min, {}/hour",
                userId, requestsInLastMinute + 1, requestsInLastHour + 1);

        return true;
    }

    /**
     * Get remaining requests for user in current minute
     */
    public int getRemainingRequestsPerMinute(UUID userId) {
        if (userId == null) {
            return 0;
        }

        RequestWindow window = userRequestWindows.get(userId);
        if (window == null) {
            return MAX_REQUESTS_PER_MINUTE;
        }

        Instant now = Instant.now();
        long used = window.countRequestsInWindow(now, WINDOW_DURATION);
        return Math.max(0, MAX_REQUESTS_PER_MINUTE - (int) used);
    }

    /**
     * Get remaining requests for user in current hour
     */
    public int getRemainingRequestsPerHour(UUID userId) {
        if (userId == null) {
            return 0;
        }

        RequestWindow window = userRequestWindows.get(userId);
        if (window == null) {
            return MAX_REQUESTS_PER_HOUR;
        }

        Instant now = Instant.now();
        long used = window.countRequestsInWindow(now, HOURLY_WINDOW_DURATION);
        return Math.max(0, MAX_REQUESTS_PER_HOUR - (int) used);
    }

    /**
     * Reset rate limit for a specific user (admin function)
     */
    public void resetUserLimit(UUID userId) {
        userRequestWindows.remove(userId);
        log.info("Rate limit reset for user {}", userId);
    }

    /**
     * Clean up old entries periodically (should be called by scheduled task)
     */
    public void cleanupOldEntries() {
        Instant cutoff = Instant.now().minus(HOURLY_WINDOW_DURATION);
        userRequestWindows.entrySet().removeIf(entry -> {
            entry.getValue().cleanupOldRequests(cutoff);
            return entry.getValue().isEmpty();
        });
        log.debug("Cleaned up rate limiting cache, remaining users: {}", userRequestWindows.size());
    }

    /**
     * Inner class to track request timestamps for a user
     */
    private static class RequestWindow {
        private final ConcurrentHashMap<Long, Integer> requestCounts = new ConcurrentHashMap<>();

        public void addRequest(Instant timestamp) {
            // Store by second for efficient counting
            long secondKey = timestamp.getEpochSecond();
            requestCounts.merge(secondKey, 1, Integer::sum);
        }

        public long countRequestsInWindow(Instant now, Duration window) {
            long cutoffSecond = now.minus(window).getEpochSecond();
            return requestCounts.entrySet().stream()
                    .filter(entry -> entry.getKey() >= cutoffSecond)
                    .mapToLong(Map.Entry::getValue)
                    .sum();
        }

        public void cleanupOldRequests(Instant cutoff) {
            long cutoffSecond = cutoff.getEpochSecond();
            requestCounts.entrySet().removeIf(entry -> entry.getKey() < cutoffSecond);
        }

        public boolean isEmpty() {
            return requestCounts.isEmpty();
        }
    }
}
