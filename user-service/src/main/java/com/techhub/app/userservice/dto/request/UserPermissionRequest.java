package com.techhub.app.userservice.dto.request;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@Data
public class UserPermissionRequest {

    @NotNull
    private UUID permissionId;

    private Boolean allowed = Boolean.TRUE;

    private Boolean active = Boolean.TRUE;
}
