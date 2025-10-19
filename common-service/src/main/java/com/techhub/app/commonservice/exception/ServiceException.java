package com.techhub.app.commonservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ServiceException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus status;

    public ServiceException(String message) {
        this(message, ErrorCode.UNKNOWN_ERROR);
    }

    public ServiceException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.status = errorCode.getHttpStatus();
    }

    public ServiceException(String message, ErrorCode errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.status = errorCode.getHttpStatus();
    }

    public String getCode() {
        return errorCode.getCode();
    }
}
