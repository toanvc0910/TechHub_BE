package com.techhub.app.commonservice.exception;

public class ForbiddenException extends ServiceException {

    public ForbiddenException(String message) {
        super(message, ErrorCode.FORBIDDEN);
    }
}
