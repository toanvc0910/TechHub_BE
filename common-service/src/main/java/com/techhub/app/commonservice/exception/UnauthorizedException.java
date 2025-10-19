package com.techhub.app.commonservice.exception;

public class UnauthorizedException extends ServiceException {

    public UnauthorizedException(String message) {
        super(message, ErrorCode.UNAUTHORIZED);
    }
}
