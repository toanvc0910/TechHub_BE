package com.techhub.app.proxyclient.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PermissionCheckRequest {
    private String url;
    private String method; // MUST match PermissionMethod enum values on user-service (GET,POST,PUT,DELETE,PATCH,OPTIONS)
}
