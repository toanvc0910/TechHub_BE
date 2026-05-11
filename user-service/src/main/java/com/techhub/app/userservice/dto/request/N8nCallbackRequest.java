package com.techhub.app.userservice.dto.request;

import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
public class N8nCallbackRequest {
    private UUID applicationId;
    /** "PROCESSED" hoặc "FAILED" */
    private String status;
    private Map<String, Object> data;
    private String error;
}
