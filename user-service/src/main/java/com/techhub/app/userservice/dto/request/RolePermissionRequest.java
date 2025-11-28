package com.techhub.app.userservice.dto.request;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

@Data
public class RolePermissionRequest {
    @NotEmpty
    private List<UUID> permissionIds;
}
