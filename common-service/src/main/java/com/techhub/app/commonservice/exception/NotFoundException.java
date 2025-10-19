package com.techhub.app.commonservice.exception;

public class NotFoundException extends ServiceException {

    public NotFoundException(String message) {
        super(message, ErrorCode.RESOURCE_NOT_FOUND);
    }
}
