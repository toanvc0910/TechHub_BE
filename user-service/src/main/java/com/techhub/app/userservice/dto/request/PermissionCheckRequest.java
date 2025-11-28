package com.techhub.app.userservice.dto.request;

import com.techhub.app.userservice.enums.PermissionMethod;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class PermissionCheckRequest {

    @NotBlank
    private String url;

    @NotNull
    private PermissionMethod method;
}
