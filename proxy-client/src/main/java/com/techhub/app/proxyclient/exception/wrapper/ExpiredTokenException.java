package com.techhub.app.proxyclient.exception.wrapper;

public class ExpiredTokenException extends RuntimeException {

    public ExpiredTokenException() {
        super();
    }

    public ExpiredTokenException(String message) {
        super(message);
    }
}
