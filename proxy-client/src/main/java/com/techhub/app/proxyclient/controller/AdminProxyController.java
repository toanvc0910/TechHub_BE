package com.techhub.app.proxyclient.controller;

import com.techhub.app.proxyclient.cache.PermissionCacheService;
import com.techhub.app.proxyclient.client.UserServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/proxy/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminProxyController {

    private final UserServiceClient userServiceClient;
    private final PermissionCacheService permissionCacheService;

    @GetMapping("/permissions")
    public ResponseEntity<String> listPermissions(@RequestHeader("Authorization") String authHeader) {
        return userServiceClient.listPermissions(authHeader);
    }

    @GetMapping("/permissions/{permissionId}")
    public ResponseEntity<String> getPermissionById(@PathVariable String permissionId,
            @RequestHeader("Authorization") String authHeader) {
        return userServiceClient.getPermissionById(permissionId, authHeader);
    }

    @DeleteMapping("/permissions/{permissionId}")
    public ResponseEntity<String> deletePermission(@PathVariable String permissionId,
            @RequestHeader("Authorization") String authHeader) {
        return userServiceClient.deletePermission(permissionId, authHeader);
    }

    @PostMapping("/permissions")
    public ResponseEntity<String> createPermission(@RequestBody Object body,
            @RequestHeader("Authorization") String authHeader) {
        return userServiceClient.createPermission(body, authHeader);
    }

    @PutMapping("/permissions/{permissionId}")
    public ResponseEntity<String> updatePermission(@PathVariable String permissionId,
            @RequestBody Object body,
            @RequestHeader("Authorization") String authHeader) {
        return userServiceClient.updatePermission(permissionId, body, authHeader);
    }

    @GetMapping("/roles")
    public ResponseEntity<String> listRoles(@RequestHeader("Authorization") String authHeader) {
        return userServiceClient.listRoles(authHeader);
    }

    @GetMapping("/roles/{roleId}")
    public ResponseEntity<String> getRoleById(@PathVariable String roleId,
            @RequestHeader("Authorization") String authHeader) {
        return userServiceClient.getRoleById(roleId, authHeader);
    }

    @PostMapping("/roles")
    public ResponseEntity<String> createRole(@RequestBody Object body,
            @RequestHeader("Authorization") String authHeader) {
        return userServiceClient.createRole(body, authHeader);
    }

    @PutMapping("/roles/{roleId}")
    public ResponseEntity<String> updateRole(@PathVariable String roleId,
            @RequestBody Object body,
            @RequestHeader("Authorization") String authHeader) {
        return userServiceClient.updateRole(roleId, body, authHeader);
    }

    @DeleteMapping("/roles/{roleId}")
    public ResponseEntity<String> deleteRole(@PathVariable String roleId,
            @RequestHeader("Authorization") String authHeader) {
        return userServiceClient.deleteRole(roleId, authHeader);
    }

    @PostMapping("/roles/{roleId}/permissions")
    public ResponseEntity<String> assignPermissionsToRole(@PathVariable String roleId,
            @RequestBody Object body,
            @RequestHeader("Authorization") String authHeader) {
        return userServiceClient.assignPermissionsToRole(roleId, body, authHeader);
    }

    @DeleteMapping("/roles/{roleId}/permissions/{permissionId}")
    public ResponseEntity<String> removePermissionFromRole(@PathVariable String roleId,
            @PathVariable String permissionId,
            @RequestHeader("Authorization") String authHeader) {
        return userServiceClient.removePermissionFromRole(roleId, permissionId, authHeader);
    }

    @GetMapping("/users/{userId}/roles")
    public ResponseEntity<String> getUserRoles(@PathVariable String userId,
            @RequestHeader("Authorization") String authHeader) {
        return userServiceClient.getUserRoles(userId, authHeader);
    }

    @PostMapping("/users/{userId}/roles")
    public ResponseEntity<String> assignRolesToUser(@PathVariable String userId,
            @RequestBody Object body,
            @RequestHeader("Authorization") String authHeader) {
        return userServiceClient.assignRolesToUser(userId, body, authHeader);
    }

    @DeleteMapping("/users/{userId}/roles/{roleId}")
    public ResponseEntity<String> removeRoleFromUser(@PathVariable String userId,
            @PathVariable String roleId,
            @RequestHeader("Authorization") String authHeader) {
        return userServiceClient.removeRoleFromUser(userId, roleId, authHeader);
    }

    // ===== Cache Management (Debug) =====

    @DeleteMapping("/cache/permissions/{userId}")
    public ResponseEntity<String> clearUserPermissionsCache(@PathVariable String userId) {
        try {
            log.info("🗑️ [AdminProxyController] Clearing permission cache for user: {}", userId);
            permissionCacheService.clearUserPermissions(UUID.fromString(userId));
            return ResponseEntity.ok("{\"message\": \"Cache cleared for user: " + userId + "\"}");
        } catch (Exception e) {
            log.error("❌ [AdminProxyController] Error clearing cache for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"Failed to clear cache: " + e.getMessage() + "\"}");
        }
    }

    @DeleteMapping("/cache/permissions")
    public ResponseEntity<String> clearAllPermissionsCache() {
        try {
            log.info("🗑️ [AdminProxyController] Clearing ALL permission caches");
            permissionCacheService.clearAllPermissions();
            return ResponseEntity.ok("{\"message\": \"All permission caches cleared\"}");
        } catch (Exception e) {
            log.error("❌ [AdminProxyController] Error clearing all caches: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"Failed to clear caches: " + e.getMessage() + "\"}");
        }
    }
}
