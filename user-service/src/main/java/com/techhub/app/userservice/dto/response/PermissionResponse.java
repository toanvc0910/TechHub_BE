package com.techhub.app.userservice.dto.response;

import com.techhub.app.userservice.enums.PermissionMethod;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class PermissionResponse {
    private UUID id;
    private String name;
    private String description;
    private String url;
    private PermissionMethod method;
    private String resource;
    private String source; // ROLE or USER_OVERRIDE
    private Boolean allowed;
}
