package com.techhub.app.userservice.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class RoleUpsertRequest {
    @NotBlank
    private String name;
    private String description;
    private Boolean active = Boolean.TRUE;
}
