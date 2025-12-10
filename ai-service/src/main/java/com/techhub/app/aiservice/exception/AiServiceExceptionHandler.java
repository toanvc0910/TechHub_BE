package com.techhub.app.aiservice.exception;

import com.techhub.app.commonservice.payload.GlobalResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for AI Service
 */
@RestControllerAdvice
@Slf4j
public class AiServiceExceptionHandler {

    /**
     * Handle prompt injection attempts
     */
    @ExceptionHandler(PromptInjectionException.class)
    public ResponseEntity<GlobalResponse<Object>> handlePromptInjection(
            PromptInjectionException ex,
            HttpServletRequest request) {

        log.warn("🚨 Prompt injection detected: {} from IP: {}",
                ex.getMessage(), request.getRemoteAddr());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(GlobalResponse
                        .<Object>error("Your message contains prohibited patterns. Please rephrase your question.", 400)
                        .withStatus("PROMPT_INJECTION_DETECTED")
                        .withPath(request.getRequestURI()));
    }

    /**
     * Handle rate limit exceeded
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<GlobalResponse<Map<String, Object>>> handleRateLimitExceeded(
            RateLimitExceededException ex,
            HttpServletRequest request) {

        log.warn("⚠️ Rate limit exceeded: {} from IP: {}",
                ex.getMessage(), request.getRemoteAddr());

        Map<String, Object> details = new HashMap<>();
        details.put("remainingPerMinute", ex.getRemainingRequestsPerMinute());
        details.put("remainingPerHour", ex.getRemainingRequestsPerHour());
        details.put("message", "You have exceeded the rate limit. Please wait before making more requests.");

        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(GlobalResponse.<Map<String, Object>>success(ex.getMessage(), details)
                        .withStatus("RATE_LIMIT_EXCEEDED")
                        .withPath(request.getRequestURI()));
    }

    /**
     * Handle validation errors (including @SafePrompt)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GlobalResponse<Map<String, String>>> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value"));

        log.warn("Validation failed: {} from IP: {}", errors, request.getRemoteAddr());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(GlobalResponse.<Map<String, String>>success("Validation failed for request", errors)
                        .withStatus("VALIDATION_ERROR")
                        .withPath(request.getRequestURI()));
    }

    /**
     * Handle constraint violations
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<GlobalResponse<Map<String, String>>> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        Map<String, String> errors = ex.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage));

        log.warn("Constraint violation: {} from IP: {}", errors, request.getRemoteAddr());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(GlobalResponse.<Map<String, String>>success("Request validation failed", errors)
                        .withStatus("CONSTRAINT_VIOLATION")
                        .withPath(request.getRequestURI()));
    }

    /**
     * Handle illegal argument exceptions (from sanitization service)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<GlobalResponse<Object>> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        log.warn("Invalid argument: {} from IP: {}", ex.getMessage(), request.getRemoteAddr());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(GlobalResponse.<Object>error(ex.getMessage(), 400)
                        .withStatus("INVALID_ARGUMENT")
                        .withPath(request.getRequestURI()));
    }

    /**
     * Handle generic exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<GlobalResponse<Object>> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unexpected error: {} from IP: {}", ex.getMessage(), request.getRemoteAddr(), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(GlobalResponse.<Object>error("An unexpected error occurred. Please try again later.", 500)
                        .withStatus("INTERNAL_ERROR")
                        .withPath(request.getRequestURI()));
    }
}
