package com.techhub.app.paymentservice.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class RestResponseObject <T> extends ResponseEntity<RestResponseObject.Payload<T>> {
    public RestResponseObject(HttpStatus status, String message, T data) {
        super(new Payload<>(status.value(), message, data), status);
    }
    @Builder
    public static class Payload<T> {
        public int code;
        public String message;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public T data;
    }
}
