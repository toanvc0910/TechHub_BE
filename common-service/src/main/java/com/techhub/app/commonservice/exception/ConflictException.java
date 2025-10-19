package com.techhub.app.commonservice.exception;

public class ConflictException extends ServiceException {

    public ConflictException(String message) {
        super(message, ErrorCode.CONFLICT);
    }
}
