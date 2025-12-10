package com.techhub.app.aiservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Security configuration properties for AI Service
 */
@Configuration
@ConfigurationProperties(prefix = "ai.security")
@Data
public class AiSecurityProperties {

    /**
     * Rate limiting configuration
     */
    private RateLimit rateLimit = new RateLimit();

    /**
     * Prompt validation configuration
     */
    private PromptValidation promptValidation = new PromptValidation();

    @Data
    public static class RateLimit {
        /**
         * Maximum requests per minute per user
         */
        private int maxRequestsPerMinute = 20;

        /**
         * Maximum requests per hour per user
         */
        private int maxRequestsPerHour = 100;

        /**
         * Enable rate limiting
         */
        private boolean enabled = true;
    }

    @Data
    public static class PromptValidation {
        /**
         * Maximum message length in characters
         */
        private int maxMessageLength = 2000;

        /**
         * Maximum consecutive special characters allowed
         */
        private int maxConsecutiveSpecialChars = 5;

        /**
         * Enable prompt injection detection
         */
        private boolean enabled = true;

        /**
         * Log prompt injection attempts
         */
        private boolean logAttempts = true;
    }
}
