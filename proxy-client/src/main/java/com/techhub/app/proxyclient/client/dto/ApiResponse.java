package com.techhub.app.proxyclient.client.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

@Data
public class ApiResponse {
    private boolean success;
    private String message;
    private Object data;
    private String errorCode;

    public boolean isSuccess() {
        return success;
    }

    public <T> T getData(Class<T> clazz) {
        if (data == null) return null;
        ObjectMapper mapper = new ObjectMapper();
        return mapper.convertValue(data, clazz);
    }
}

