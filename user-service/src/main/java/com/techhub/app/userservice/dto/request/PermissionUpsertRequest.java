package com.techhub.app.userservice.dto.request;

import com.techhub.app.userservice.enums.PermissionMethod;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class PermissionUpsertRequest {
    @NotBlank
    private String name;
    private String description;
    @NotBlank
    private String url;
    @NotNull
    private PermissionMethod method;
    @NotBlank
    private String resource;
    private Boolean active = Boolean.TRUE;
}
