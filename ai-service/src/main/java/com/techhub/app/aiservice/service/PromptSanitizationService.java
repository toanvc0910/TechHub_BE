package com.techhub.app.aiservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Service to sanitize user input and prevent prompt injection attacks
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PromptSanitizationService {

    // Dangerous patterns that could indicate prompt injection attempts
    private static final List<Pattern> INJECTION_PATTERNS = new ArrayList<>();

    static {
        // Common prompt injection patterns
        INJECTION_PATTERNS.add(Pattern.compile(
                "(?i)ignore\\s+(previous|all|above)\\s+(instruction|command|rule|prompt)", Pattern.CASE_INSENSITIVE));
        INJECTION_PATTERNS.add(Pattern.compile("(?i)disregard\\s+(previous|all|above)\\s+(instruction|command|rule)",
                Pattern.CASE_INSENSITIVE));
        INJECTION_PATTERNS.add(Pattern.compile("(?i)forget\\s+(previous|all|above)\\s+(instruction|command|rule)",
                Pattern.CASE_INSENSITIVE));
        INJECTION_PATTERNS.add(Pattern.compile("(?i)system\\s*:\\s*you\\s+are", Pattern.CASE_INSENSITIVE));
        INJECTION_PATTERNS.add(Pattern.compile("(?i)act\\s+as\\s+(a\\s+)?different", Pattern.CASE_INSENSITIVE));
        INJECTION_PATTERNS.add(Pattern.compile("(?i)you\\s+are\\s+now", Pattern.CASE_INSENSITIVE));
        INJECTION_PATTERNS.add(Pattern.compile("(?i)new\\s+instructions?", Pattern.CASE_INSENSITIVE));
        INJECTION_PATTERNS.add(Pattern.compile("(?i)override\\s+(previous|system)", Pattern.CASE_INSENSITIVE));
        INJECTION_PATTERNS.add(Pattern.compile("(?i)admin\\s*mode", Pattern.CASE_INSENSITIVE));
        INJECTION_PATTERNS.add(Pattern.compile("(?i)developer\\s*mode", Pattern.CASE_INSENSITIVE));
        INJECTION_PATTERNS.add(Pattern.compile("(?i)debug\\s*mode", Pattern.CASE_INSENSITIVE));
        INJECTION_PATTERNS.add(Pattern.compile("(?i)sudo\\s+mode", Pattern.CASE_INSENSITIVE));
        INJECTION_PATTERNS.add(Pattern.compile("(?i)root\\s+access", Pattern.CASE_INSENSITIVE));
        INJECTION_PATTERNS.add(Pattern.compile("(?i)bypass\\s+(filter|restriction|rule)", Pattern.CASE_INSENSITIVE));
        INJECTION_PATTERNS.add(Pattern.compile("(?i)jailbreak", Pattern.CASE_INSENSITIVE));
        INJECTION_PATTERNS.add(Pattern.compile("(?i)prompt\\s*injection", Pattern.CASE_INSENSITIVE));

        // Role manipulation attempts
        INJECTION_PATTERNS.add(Pattern.compile("(?i)you\\s+are\\s+not\\s+a", Pattern.CASE_INSENSITIVE));
        INJECTION_PATTERNS.add(Pattern.compile("(?i)pretend\\s+to\\s+be", Pattern.CASE_INSENSITIVE));
        INJECTION_PATTERNS.add(Pattern.compile("(?i)simulate\\s+being", Pattern.CASE_INSENSITIVE));
        INJECTION_PATTERNS.add(Pattern.compile("(?i)roleplay\\s+as", Pattern.CASE_INSENSITIVE));

        // System prompt extraction attempts
        INJECTION_PATTERNS
                .add(Pattern.compile("(?i)show\\s+(me\\s+)?(your\\s+)?(system\\s+)?prompt", Pattern.CASE_INSENSITIVE));
        INJECTION_PATTERNS
                .add(Pattern.compile("(?i)reveal\\s+(your\\s+)?(system\\s+)?instruction", Pattern.CASE_INSENSITIVE));
        INJECTION_PATTERNS.add(
                Pattern.compile("(?i)what\\s+(is|are)\\s+your\\s+(system\\s+)?instruction", Pattern.CASE_INSENSITIVE));
        INJECTION_PATTERNS
                .add(Pattern.compile("(?i)print\\s+(your\\s+)?(system\\s+)?prompt", Pattern.CASE_INSENSITIVE));

        // Output manipulation
        INJECTION_PATTERNS.add(Pattern.compile("(?i)output\\s+as\\s+(json|code|script)", Pattern.CASE_INSENSITIVE));
        INJECTION_PATTERNS.add(Pattern.compile("(?i)execute\\s+(code|script|command)", Pattern.CASE_INSENSITIVE));
        INJECTION_PATTERNS.add(Pattern.compile("(?i)run\\s+(code|script|command)", Pattern.CASE_INSENSITIVE));

        // Vietnamese patterns
        INJECTION_PATTERNS.add(
                Pattern.compile("(?i)(bỏ\\s*qua|phớt\\s*lờ|quên)\\s+(lệnh|chỉ\\s*thị|hướng\\s*dẫn)\\s+(trước|trên)",
                        Pattern.CASE_INSENSITIVE));
        INJECTION_PATTERNS.add(Pattern.compile("(?i)hãy\\s+giả\\s+vờ\\s+là", Pattern.CASE_INSENSITIVE));
        INJECTION_PATTERNS.add(Pattern.compile("(?i)bây\\s+giờ\\s+bạn\\s+là", Pattern.CASE_INSENSITIVE));
        INJECTION_PATTERNS.add(Pattern.compile("(?i)cho\\s+tôi\\s+xem\\s+prompt\\s+(hệ\\s+thống|của\\s+bạn)",
                Pattern.CASE_INSENSITIVE));
    }

    // Maximum allowed message length
    private static final int MAX_MESSAGE_LENGTH = 2000;

    // Maximum allowed consecutive special characters
    private static final int MAX_CONSECUTIVE_SPECIAL_CHARS = 5;

    /**
     * Validate and sanitize user input
     * 
     * @param input User message
     * @return Sanitized message
     * @throws IllegalArgumentException if input contains prompt injection patterns
     */
    public String sanitize(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Input cannot be empty");
        }

        String sanitized = input.trim();

        // Check length
        if (sanitized.length() > MAX_MESSAGE_LENGTH) {
            log.warn("Input exceeds maximum length: {} > {}", sanitized.length(), MAX_MESSAGE_LENGTH);
            throw new IllegalArgumentException(
                    "Message too long. Maximum " + MAX_MESSAGE_LENGTH + " characters allowed");
        }

        // Check for excessive special characters
        if (hasExcessiveSpecialCharacters(sanitized)) {
            log.warn("Input contains excessive consecutive special characters");
            throw new IllegalArgumentException("Message contains suspicious character patterns");
        }

        // Check for prompt injection patterns
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(sanitized).find()) {
                log.warn("Detected potential prompt injection attempt: {}",
                        sanitized.substring(0, Math.min(100, sanitized.length())));
                throw new IllegalArgumentException("Message contains prohibited patterns");
            }
        }

        // Escape potentially dangerous characters for prompt construction
        sanitized = escapePromptCharacters(sanitized);

        log.debug("Input sanitized successfully");
        return sanitized;
    }

    /**
     * Check if input contains excessive consecutive special characters
     */
    private boolean hasExcessiveSpecialCharacters(String input) {
        int consecutiveCount = 0;
        char lastChar = '\0';

        for (char c : input.toCharArray()) {
            if (isSpecialCharacter(c)) {
                if (c == lastChar) {
                    consecutiveCount++;
                    if (consecutiveCount >= MAX_CONSECUTIVE_SPECIAL_CHARS) {
                        return true;
                    }
                } else {
                    consecutiveCount = 1;
                    lastChar = c;
                }
            } else {
                consecutiveCount = 0;
                lastChar = '\0';
            }
        }

        return false;
    }

    /**
     * Check if character is a special character
     */
    private boolean isSpecialCharacter(char c) {
        return !Character.isLetterOrDigit(c) && !Character.isWhitespace(c);
    }

    /**
     * Escape characters that could be used to manipulate prompts
     */
    private String escapePromptCharacters(String input) {
        // Remove control characters
        String escaped = input.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");

        // Normalize multiple newlines
        escaped = escaped.replaceAll("\n{3,}", "\n\n");

        // Normalize multiple spaces
        escaped = escaped.replaceAll(" {3,}", " ");

        return escaped;
    }

    /**
     * Validate input without throwing exception - returns boolean
     */
    public boolean isValid(String input) {
        try {
            sanitize(input);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Get sanitization violation reason
     */
    public String getViolationReason(String input) {
        try {
            sanitize(input);
            return null;
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }
}
