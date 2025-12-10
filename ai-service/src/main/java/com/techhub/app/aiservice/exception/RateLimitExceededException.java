package com.techhub.app.aiservice.exception;

/**
 * Exception thrown when rate limit is exceeded
 */
public class RateLimitExceededException extends RuntimeException {

    private final int remainingRequestsPerMinute;
    private final int remainingRequestsPerHour;

    public RateLimitExceededException(String message, int remainingPerMinute, int remainingPerHour) {
        super(message);
        this.remainingRequestsPerMinute = remainingPerMinute;
        this.remainingRequestsPerHour = remainingPerHour;
    }

    public int getRemainingRequestsPerMinute() {
        return remainingRequestsPerMinute;
    }

    public int getRemainingRequestsPerHour() {
        return remainingRequestsPerHour;
    }
}
