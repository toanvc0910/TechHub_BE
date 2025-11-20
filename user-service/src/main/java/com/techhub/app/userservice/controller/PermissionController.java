package com.techhub.app.userservice.controller;

import com.techhub.app.commonservice.payload.GlobalResponse;
import com.techhub.app.userservice.dto.request.PermissionCheckRequest;
import com.techhub.app.userservice.dto.request.UserPermissionRequest;
import com.techhub.app.userservice.dto.response.PermissionResponse;
import com.techhub.app.userservice.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users/{userId}/permissions")
@RequiredArgsConstructor
@Slf4j
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping("/effective")
    public ResponseEntity<GlobalResponse<List<PermissionResponse>>> getEffectivePermissions(
            @PathVariable UUID userId,
            HttpServletRequest request) {
        try {
            List<PermissionResponse> permissions = permissionService.getEffectivePermissions(userId);
            return ResponseEntity.ok(
                    GlobalResponse.success("Effective permissions retrieved", permissions)
                            .withPath(request.getRequestURI()));
        } catch (Exception e) {
            log.error("Failed to get effective permissions for {}", userId, e);
            return ResponseEntity.badRequest()
                    .body(GlobalResponse.<List<PermissionResponse>>error(e.getMessage(), 400)
                            .withPath(request.getRequestURI()));
        }
    }

    @PostMapping("/check")
    public ResponseEntity<GlobalResponse<Boolean>> checkPermission(
            @PathVariable UUID userId,
            @Valid @RequestBody PermissionCheckRequest request,
            HttpServletRequest httpRequest) {
        try {
            boolean allowed = permissionService.hasPermission(userId, request.getUrl(), request.getMethod());
            return ResponseEntity.ok(
                    GlobalResponse.success("Permission evaluated", allowed)
                            .withPath(httpRequest.getRequestURI()));
        } catch (Exception e) {
            log.error("Failed to check permission for {} on {} {}", userId, request.getMethod(), request.getUrl(), e);
            return ResponseEntity.badRequest()
                    .body(GlobalResponse.<Boolean>error(e.getMessage(), 400)
                            .withPath(httpRequest.getRequestURI()));
        }
    }

    @PostMapping
    public ResponseEntity<GlobalResponse<PermissionResponse>> upsertUserPermission(
            @PathVariable UUID userId,
            @Valid @RequestBody UserPermissionRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String actorHeader,
            HttpServletRequest httpRequest) {
        try {
            UUID actorId = parseUuid(actorHeader);

            PermissionResponse response = permissionService.upsertUserPermission(
                    userId,
                    request.getPermissionId(),
                    Boolean.TRUE.equals(request.getAllowed()),
                    Boolean.TRUE.equals(request.getActive()),
                    actorId);

            return ResponseEntity.ok(
                    GlobalResponse.success("User permission saved", response)
                            .withPath(httpRequest.getRequestURI()));
        } catch (Exception e) {
            log.error("Failed to upsert user permission for {} - {}", userId, request.getPermissionId(), e);
            return ResponseEntity.badRequest()
                    .body(GlobalResponse.<PermissionResponse>error(e.getMessage(), 400)
                            .withPath(httpRequest.getRequestURI()));
        }
    }

    @DeleteMapping("/{permissionId}")
    public ResponseEntity<GlobalResponse<?>> deactivateUserPermission(
            @PathVariable UUID userId,
            @PathVariable UUID permissionId,
            @RequestHeader(value = "X-User-Id", required = false) String actorHeader,
            HttpServletRequest httpRequest) {
        try {
            UUID actorId = parseUuid(actorHeader);
            permissionService.deactivateUserPermission(userId, permissionId, actorId);
            return ResponseEntity.ok(
                    GlobalResponse.success("User permission deactivated", null)
                            .withPath(httpRequest.getRequestURI()));
        } catch (Exception e) {
            log.error("Failed to deactivate user permission {} for user {}", permissionId, userId, e);
            return ResponseEntity.badRequest()
                    .body(GlobalResponse.<Object>error(e.getMessage(), 400)
                            .withPath(httpRequest.getRequestURI()));
        }
    }

    private UUID parseUuid(String raw) {
        try {
            return raw != null && !raw.isBlank() ? UUID.fromString(raw) : null;
        } catch (IllegalArgumentException e) {
            log.warn("Invalid UUID received in header: {}", raw);
            return null;
        }
    }
}
