package com.techhub.app.userservice.dto.request;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

@Data
public class UserRoleRequest {
    @NotEmpty
    private List<UUID> roleIds;
    private Boolean active = Boolean.TRUE;
}
