package com.techhub.app.aiservice.exception;

/**
 * Exception thrown when prompt injection is detected
 */
public class PromptInjectionException extends RuntimeException {

    public PromptInjectionException(String message) {
        super(message);
    }

    public PromptInjectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
