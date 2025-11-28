package com.techhub.app.userservice.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;

@Data
public class RoleUpsertRequest {
    @NotBlank
    private String name;
    private String description;
    private Boolean active = Boolean.TRUE;
    private List<UUID> permissionIds; // Permission IDs to assign to the role
}
