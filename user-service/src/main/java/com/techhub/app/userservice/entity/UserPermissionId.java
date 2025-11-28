package com.techhub.app.userservice.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPermissionId implements Serializable {
    private UUID userId;
    private UUID permissionId;
}
