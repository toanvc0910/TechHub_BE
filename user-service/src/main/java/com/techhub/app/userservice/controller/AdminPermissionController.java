package com.techhub.app.userservice.controller;

import com.techhub.app.commonservice.payload.GlobalResponse;
import com.techhub.app.userservice.dto.request.PermissionUpsertRequest;
import com.techhub.app.userservice.dto.request.RolePermissionRequest;
import com.techhub.app.userservice.dto.request.RoleUpsertRequest;
import com.techhub.app.userservice.dto.request.UserRoleRequest;
import com.techhub.app.userservice.dto.response.PermissionResponse;
import com.techhub.app.userservice.dto.response.RoleResponse;
import com.techhub.app.userservice.enums.PermissionMethod;
import com.techhub.app.userservice.service.PermissionService;
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

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminPermissionController {

        private final PermissionService permissionService;

        // ===== Permission CRUD =====
        @GetMapping("/permissions")
        public ResponseEntity<GlobalResponse<List<PermissionResponse>>> listPermissions(HttpServletRequest request) {
                List<PermissionResponse> permissions = permissionService.listPermissions();
                return ResponseEntity.ok(
                                GlobalResponse.success("Permissions fetched", permissions)
                                                .withPath(request.getRequestURI()));
        }

        @GetMapping("/permissions/{permissionId}")
        public ResponseEntity<GlobalResponse<PermissionResponse>> getPermissionById(
                        @PathVariable UUID permissionId,
                        HttpServletRequest request) {
                PermissionResponse permission = permissionService.getPermissionById(permissionId);
                return ResponseEntity.ok(
                                GlobalResponse.success("Permission fetched", permission)
                                                .withPath(request.getRequestURI()));
        }

        @PostMapping("/permissions")
        public ResponseEntity<GlobalResponse<PermissionResponse>> createPermission(
                        @Valid @RequestBody PermissionUpsertRequest body,
                        @RequestHeader(value = "X-User-Id", required = false) String actorHeader,
                        HttpServletRequest request) {
                UUID actor = parseUuid(actorHeader);
                PermissionResponse res = permissionService.createPermission(
                                body.getName(),
                                body.getDescription(),
                                body.getUrl(),
                                body.getMethod(),
                                body.getResource(),
                                Boolean.TRUE.equals(body.getActive()),
                                actor);
                return ResponseEntity.ok(
                                GlobalResponse.success("Permission created", res)
                                                .withPath(request.getRequestURI()));
        }

        @PutMapping("/permissions/{permissionId}")
        public ResponseEntity<GlobalResponse<PermissionResponse>> updatePermission(
                        @PathVariable UUID permissionId,
                        @Valid @RequestBody PermissionUpsertRequest body,
                        @RequestHeader(value = "X-User-Id", required = false) String actorHeader,
                        HttpServletRequest request) {
                UUID actor = parseUuid(actorHeader);
                PermissionResponse res = permissionService.updatePermission(
                                permissionId,
                                body.getName(),
                                body.getDescription(),
                                body.getUrl(),
                                body.getMethod(),
                                body.getResource(),
                                Boolean.TRUE.equals(body.getActive()),
                                actor);
                return ResponseEntity.ok(
                                GlobalResponse.success("Permission updated", res)
                                                .withPath(request.getRequestURI()));
        }

        @DeleteMapping("/permissions/{permissionId}")
        public ResponseEntity<GlobalResponse<?>> deletePermission(
                        @PathVariable UUID permissionId,
                        @RequestHeader(value = "X-User-Id", required = false) String actorHeader,
                        HttpServletRequest request) {
                UUID actor = parseUuid(actorHeader);
                log.info("[DELETE PERMISSION] Starting deletion - PermissionId: {}, ActorId: {}", permissionId, actor);
                try {
                        permissionService.deletePermission(permissionId, actor);
                        log.info("[DELETE PERMISSION] Successfully deleted - PermissionId: {}", permissionId);
                        return ResponseEntity.ok(
                                        GlobalResponse.success("Permission deleted", null)
                                                        .withPath(request.getRequestURI()));
                } catch (Exception e) {
                        log.error("[DELETE PERMISSION] Failed - PermissionId: {}, Error: {}", permissionId,
                                        e.getMessage(), e);
                        throw e;
                }
        }

        // ===== Role CRUD + assign permissions =====
        @GetMapping("/roles")
        public ResponseEntity<GlobalResponse<List<RoleResponse>>> listRoles(HttpServletRequest request) {
                List<RoleResponse> roles = permissionService.listRoles();
                return ResponseEntity.ok(
                                GlobalResponse.success("Roles fetched", roles)
                                                .withPath(request.getRequestURI()));
        }

        @GetMapping("/roles/{roleId}")
        public ResponseEntity<GlobalResponse<RoleResponse>> getRoleById(
                        @PathVariable UUID roleId,
                        HttpServletRequest request) {
                RoleResponse role = permissionService.getRoleById(roleId);
                return ResponseEntity.ok(
                                GlobalResponse.success("Role fetched", role)
                                                .withPath(request.getRequestURI()));
        }

        @PostMapping("/roles")
        public ResponseEntity<GlobalResponse<RoleResponse>> createRole(
                        @Valid @RequestBody RoleUpsertRequest body,
                        @RequestHeader(value = "X-User-Id", required = false) String actorHeader,
                        HttpServletRequest request) {
                UUID actor = parseUuid(actorHeader);

                RoleResponse res = permissionService.createRole(
                                body.getName(),
                                body.getDescription(),
                                Boolean.TRUE.equals(body.getActive()),
                                actor,
                                body.getPermissionIds());

                log.info("[CREATE ROLE] Successfully created - RoleId: {}, Name: {}, PermissionsCount: {}",
                                res.getId(), res.getName(),
                                res.getPermissionIds() != null ? res.getPermissionIds().size() : 0);

                return ResponseEntity.ok(
                                GlobalResponse.success("Role created", res)
                                                .withPath(request.getRequestURI()));
        }

        @PutMapping("/roles/{roleId}")
        public ResponseEntity<GlobalResponse<RoleResponse>> updateRole(
                        @PathVariable UUID roleId,
                        @Valid @RequestBody RoleUpsertRequest body,
                        @RequestHeader(value = "X-User-Id", required = false) String actorHeader,
                        @RequestHeader(value = "Authorization", required = false) String authHeader,
                        HttpServletRequest request) {
                UUID actor = parseUuid(actorHeader);
                log.info("[UPDATE ROLE] Request received - RoleId: {}, Name: {}, Description: {}, Active: {}, PermissionIds: {}",
                                roleId, body.getName(), body.getDescription(), body.getActive(),
                                body.getPermissionIds());
                log.info("[UPDATE ROLE] Headers - X-User-Id: {}, Actor: {}, Authorization: {}",
                                actorHeader, actor, authHeader != null ? "Bearer ***" : "null");
                log.info("[UPDATE ROLE] Request URI: {}, Method: {}", request.getRequestURI(), request.getMethod());

                try {
                        RoleResponse res = permissionService.updateRole(
                                        roleId,
                                        body.getName(),
                                        body.getDescription(),
                                        Boolean.TRUE.equals(body.getActive()),
                                        actor,
                                        body.getPermissionIds());
                        log.info("[UPDATE ROLE] Successfully updated - RoleId: {}, Name: {}", res.getId(),
                                        res.getName());
                        return ResponseEntity.ok(
                                        GlobalResponse.success("Role updated", res)
                                                        .withPath(request.getRequestURI()));
                } catch (Exception e) {
                        log.error("[UPDATE ROLE] Failed - RoleId: {}, Error: {}", roleId, e.getMessage(), e);
                        throw e;
                }
        }

        @DeleteMapping("/roles/{roleId}")
        public ResponseEntity<GlobalResponse<?>> deleteRole(
                        @PathVariable UUID roleId,
                        @RequestHeader(value = "X-User-Id", required = false) String actorHeader,
                        HttpServletRequest request) {
                UUID actor = parseUuid(actorHeader);
                log.info("[DELETE ROLE] Starting deletion - RoleId: {}, ActorId: {}", roleId, actor);
                try {
                        permissionService.deleteRole(roleId, actor);
                        log.info("[DELETE ROLE] Successfully deleted - RoleId: {}", roleId);
                        return ResponseEntity.ok(
                                        GlobalResponse.success("Role deleted", null)
                                                        .withPath(request.getRequestURI()));
                } catch (Exception e) {
                        log.error("[DELETE ROLE] Failed - RoleId: {}, Error: {}", roleId, e.getMessage(), e);
                        throw e;
                }
        }

        @PostMapping("/roles/{roleId}/permissions")
        public ResponseEntity<GlobalResponse<?>> assignPermissionsToRole(
                        @PathVariable UUID roleId,
                        @Valid @RequestBody RolePermissionRequest body,
                        @RequestHeader(value = "X-User-Id", required = false) String actorHeader,
                        HttpServletRequest request) {
                UUID actor = parseUuid(actorHeader);
                permissionService.assignPermissionsToRole(roleId, body.getPermissionIds(), actor);
                return ResponseEntity.ok(
                                GlobalResponse.success("Permissions assigned to role", null)
                                                .withPath(request.getRequestURI()));
        }

        @DeleteMapping("/roles/{roleId}/permissions/{permissionId}")
        public ResponseEntity<GlobalResponse<?>> removePermissionFromRole(
                        @PathVariable UUID roleId,
                        @PathVariable UUID permissionId,
                        @RequestHeader(value = "X-User-Id", required = false) String actorHeader,
                        HttpServletRequest request) {
                UUID actor = parseUuid(actorHeader);
                permissionService.removePermissionFromRole(roleId, permissionId, actor);
                return ResponseEntity.ok(
                                GlobalResponse.success("Permission removed from role", null)
                                                .withPath(request.getRequestURI()));
        }

        // ===== User role assignments =====
        @GetMapping("/users/{userId}/roles")
        public ResponseEntity<GlobalResponse<List<RoleResponse>>> getUserRoles(
                        @PathVariable UUID userId,
                        HttpServletRequest request) {
                List<RoleResponse> roles = permissionService.getUserRoles(userId);
                return ResponseEntity.ok(
                                GlobalResponse.success("User roles fetched", roles)
                                                .withPath(request.getRequestURI()));
        }

        @PostMapping("/users/{userId}/roles")
        public ResponseEntity<GlobalResponse<?>> assignRolesToUser(
                        @PathVariable UUID userId,
                        @Valid @RequestBody UserRoleRequest body,
                        @RequestHeader(value = "X-User-Id", required = false) String actorHeader,
                        HttpServletRequest request) {
                UUID actor = parseUuid(actorHeader);
                permissionService.assignRolesToUser(userId, body.getRoleIds(), Boolean.TRUE.equals(body.getActive()),
                                actor);
                return ResponseEntity.ok(
                                GlobalResponse.success("Roles assigned to user", null)
                                                .withPath(request.getRequestURI()));
        }

        @DeleteMapping("/users/{userId}/roles/{roleId}")
        public ResponseEntity<GlobalResponse<?>> removeRoleFromUser(
                        @PathVariable UUID userId,
                        @PathVariable UUID roleId,
                        @RequestHeader(value = "X-User-Id", required = false) String actorHeader,
                        HttpServletRequest request) {
                UUID actor = parseUuid(actorHeader);
                permissionService.removeRoleFromUser(userId, roleId, actor);
                return ResponseEntity.ok(
                                GlobalResponse.success("Role removed from user", null)
                                                .withPath(request.getRequestURI()));
        }

        private UUID parseUuid(String raw) {
                try {
                        return raw != null && !raw.isBlank() ? UUID.fromString(raw) : null;
                } catch (IllegalArgumentException e) {
                        log.warn("Invalid UUID in header: {}", raw);
                        return null;
                }
        }
}
