package com.techhub.app.commonservice.exception;

public class BadRequestException extends ServiceException {

    public BadRequestException(String message) {
        super(message, ErrorCode.BAD_REQUEST);
    }
}
