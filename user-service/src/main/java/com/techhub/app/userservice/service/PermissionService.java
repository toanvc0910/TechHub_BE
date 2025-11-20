package com.techhub.app.userservice.service;

import com.techhub.app.userservice.dto.response.PermissionResponse;
import com.techhub.app.userservice.dto.response.RoleResponse;
import com.techhub.app.userservice.enums.PermissionMethod;

import java.util.List;
import java.util.UUID;

public interface PermissionService {

    List<PermissionResponse> getEffectivePermissions(UUID userId);

    boolean hasPermission(UUID userId, String url, PermissionMethod method);

    PermissionResponse upsertUserPermission(UUID userId, UUID permissionId, boolean allowed, boolean active, UUID actorId);

    void deactivateUserPermission(UUID userId, UUID permissionId, UUID actorId);

    // Admin/management
    List<PermissionResponse> listPermissions();

    PermissionResponse createPermission(String name, String description, String url, PermissionMethod method, String resource, boolean active, UUID actorId);

    PermissionResponse updatePermission(UUID permissionId, String name, String description, String url, PermissionMethod method, String resource, boolean active, UUID actorId);

    List<RoleResponse> listRoles();

    RoleResponse createRole(String name, String description, boolean active, UUID actorId);

    RoleResponse updateRole(UUID roleId, String name, String description, boolean active, UUID actorId);

    void assignPermissionsToRole(UUID roleId, List<UUID> permissionIds, UUID actorId);

    void removePermissionFromRole(UUID roleId, UUID permissionId, UUID actorId);

    List<RoleResponse> getUserRoles(UUID userId);

    void assignRolesToUser(UUID userId, List<UUID> roleIds, boolean active, UUID actorId);

    void removeRoleFromUser(UUID userId, UUID roleId, UUID actorId);
}
