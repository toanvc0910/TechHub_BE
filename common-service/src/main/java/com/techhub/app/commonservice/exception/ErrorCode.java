package com.techhub.app.commonservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    UNKNOWN_ERROR("UNKNOWN_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred"),
    VALIDATION_ERROR("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "Validation failed"),
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND, "Resource not found"),
    UNAUTHORIZED("UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "You are not authorized to perform this action"),
    FORBIDDEN("FORBIDDEN", HttpStatus.FORBIDDEN, "Access to the requested resource is forbidden"),
    CONFLICT("CONFLICT", HttpStatus.CONFLICT, "Request conflicts with the current state of the resource"),
    BAD_REQUEST("BAD_REQUEST", HttpStatus.BAD_REQUEST, "The request could not be processed"),
    SERVICE_UNAVAILABLE("SERVICE_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE, "Service temporarily unavailable");

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(String code, HttpStatus httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }
}