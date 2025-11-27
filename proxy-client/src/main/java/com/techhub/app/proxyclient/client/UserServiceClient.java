package com.techhub.app.proxyclient.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "USER-SERVICE")
public interface UserServiceClient {
        // Auth endpoints
        @PostMapping("/api/auth/register")
        ResponseEntity<String> register(@RequestBody Object registerRequest);

        @PostMapping("/api/auth/verify-email")
        ResponseEntity<String> verifyEmail(@RequestBody Object verifyEmailRequest);

        @PostMapping("/api/auth/resend-code")
        ResponseEntity<String> resendCode(@RequestBody Object resendCodeRequest);

        @PostMapping("/api/auth/login")
        ResponseEntity<String> login(@RequestBody Object loginRequest);

        @PostMapping("/api/auth/logout")
        ResponseEntity<String> logout(@RequestHeader("Authorization") String authHeader);

        @PostMapping("/api/auth/validate")
        ResponseEntity<String> validateToken(@RequestHeader("Authorization") String authHeader);

        @GetMapping("/api/auth/health")
        ResponseEntity<String> authHealth();

        // User management endpoints
        @GetMapping("/api/users")
        ResponseEntity<String> getAllUsers(@RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(required = false) String search,
                        @RequestHeader("Authorization") String authHeader);

        @PostMapping("/api/users")
        ResponseEntity<String> createUser(@RequestBody Object createUserRequest);

        @GetMapping("/api/users/{userId}")
        ResponseEntity<String> getUserById(@PathVariable String userId,
                        @RequestHeader("Authorization") String authHeader);

        // Internal use (for OAuth2 token exchange) - user-service endpoints are
        // permitAll
        @GetMapping("/api/users/{userId}")
        ResponseEntity<String> getUserByIdInternal(@PathVariable String userId,
                        @RequestHeader("X-Request-Source") String requestSource,
                        @RequestHeader("X-User-Id") String currentUserId,
                        @RequestHeader("X-User-Email") String currentUserEmail,
                        @RequestHeader(value = "X-User-Roles", defaultValue = "") String currentUserRoles);

        @GetMapping("/api/users/email/{email}")
        ResponseEntity<String> getUserByEmail(@PathVariable String email,
                        @RequestHeader("Authorization") String authHeader);

        @GetMapping("/api/users/username/{username}")
        ResponseEntity<String> getUserByUsername(@PathVariable String username,
                        @RequestHeader("Authorization") String authHeader);

        @PutMapping("/api/users/{userId}")
        ResponseEntity<String> updateUser(@PathVariable String userId,
                        @RequestBody Object updateRequest,
                        @RequestHeader("Authorization") String authHeader);

        @DeleteMapping("/api/users/{userId}")
        ResponseEntity<String> deleteUser(@PathVariable String userId,
                        @RequestHeader("Authorization") String authHeader);

        // Password management endpoints
        @PostMapping("/api/users/{userId}/change-password")
        ResponseEntity<String> changePassword(@PathVariable String userId,
                        @RequestBody Object changePasswordRequest,
                        @RequestHeader("Authorization") String authHeader);

        @PostMapping("/api/users/forgot-password")
        ResponseEntity<String> forgotPassword(@RequestBody Object forgotPasswordRequest);

        @PostMapping("/api/users/resend-reset-code/{email}")
        ResponseEntity<String> resendResetCode(@PathVariable String email);

        @PostMapping("/api/users/reset-password/{email}")
        ResponseEntity<String> resetPassword(@PathVariable String email,
                        @RequestBody Object resetPasswordRequest);

        // User status management endpoints
        @PostMapping("/api/users/{userId}/activate")
        ResponseEntity<String> activateUser(@PathVariable String userId,
                        @RequestHeader("Authorization") String authHeader);

        @PostMapping("/api/users/{userId}/deactivate")
        ResponseEntity<String> deactivateUser(@PathVariable String userId,
                        @RequestHeader("Authorization") String authHeader);

        @PutMapping("/api/users/{userId}/status/{status}")
        ResponseEntity<String> changeUserStatus(@PathVariable String userId,
                        @PathVariable String status,
                        @RequestHeader("Authorization") String authHeader);

        // Profile endpoint
        @GetMapping("/api/users/profile")
        ResponseEntity<String> getCurrentUserProfile(@RequestHeader("Authorization") String authHeader,
                        @RequestHeader("X-User-Id") String userId,
                        @RequestHeader("X-User-Email") String userEmail);

        // Public endpoints
        @GetMapping("/api/users/public/instructors")
        ResponseEntity<String> getPublicInstructors(@RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "4") int size);

        // ===== Admin RBAC =====
        @GetMapping("/api/admin/permissions")
        ResponseEntity<String> listPermissions(@RequestHeader("Authorization") String authHeader);

        @GetMapping("/api/admin/permissions/{permissionId}")
        ResponseEntity<String> getPermissionById(@PathVariable String permissionId,
                        @RequestHeader("Authorization") String authHeader);

        @PostMapping("/api/admin/permissions")
        ResponseEntity<String> createPermission(@RequestBody Object body,
                        @RequestHeader("Authorization") String authHeader);

        @PutMapping("/api/admin/permissions/{permissionId}")
        ResponseEntity<String> updatePermission(@PathVariable String permissionId,
                        @RequestBody Object body,
                        @RequestHeader("Authorization") String authHeader);

        @DeleteMapping("/api/admin/permissions/{permissionId}")
        ResponseEntity<String> deletePermission(@PathVariable String permissionId,
                        @RequestHeader("Authorization") String authHeader);

        @GetMapping("/api/admin/roles")
        ResponseEntity<String> listRoles(@RequestHeader("Authorization") String authHeader);

        @GetMapping("/api/admin/roles/{roleId}")
        ResponseEntity<String> getRoleById(@PathVariable String roleId,
                        @RequestHeader("Authorization") String authHeader);

        @PostMapping("/api/admin/roles")
        ResponseEntity<String> createRole(@RequestBody Object body,
                        @RequestHeader("Authorization") String authHeader);

        @PutMapping("/api/admin/roles/{roleId}")
        ResponseEntity<String> updateRole(@PathVariable String roleId,
                        @RequestBody Object body,
                        @RequestHeader("Authorization") String authHeader);

        @DeleteMapping("/api/admin/roles/{roleId}")
        ResponseEntity<String> deleteRole(@PathVariable String roleId,
                        @RequestHeader("Authorization") String authHeader);

        @PostMapping("/api/admin/roles/{roleId}/permissions")
        ResponseEntity<String> assignPermissionsToRole(@PathVariable String roleId,
                        @RequestBody Object body,
                        @RequestHeader("Authorization") String authHeader);

        @DeleteMapping("/api/admin/roles/{roleId}/permissions/{permissionId}")
        ResponseEntity<String> removePermissionFromRole(@PathVariable String roleId,
                        @PathVariable String permissionId,
                        @RequestHeader("Authorization") String authHeader);

        @GetMapping("/api/admin/users/{userId}/roles")
        ResponseEntity<String> getUserRoles(@PathVariable String userId,
                        @RequestHeader("Authorization") String authHeader);

        @PostMapping("/api/admin/users/{userId}/roles")
        ResponseEntity<String> assignRolesToUser(@PathVariable String userId,
                        @RequestBody Object body,
                        @RequestHeader("Authorization") String authHeader);

        @DeleteMapping("/api/admin/users/{userId}/roles/{roleId}")
        ResponseEntity<String> removeRoleFromUser(@PathVariable String userId,
                        @PathVariable String roleId,
                        @RequestHeader("Authorization") String authHeader);

        // User permissions endpoints
        @GetMapping("/api/users/{userId}/permissions/effective")
        ResponseEntity<String> getEffectivePermissions(@PathVariable String userId,
                        @RequestHeader("Authorization") String authHeader);

        @PostMapping("/api/users/{userId}/permissions")
        ResponseEntity<String> upsertUserPermission(@PathVariable String userId,
                        @RequestBody Object body,
                        @RequestHeader("Authorization") String authHeader);

        @DeleteMapping("/api/users/{userId}/permissions/{permissionId}")
        ResponseEntity<String> deactivateUserPermission(@PathVariable String userId,
                        @PathVariable String permissionId,
                        @RequestHeader("Authorization") String authHeader);

        // Permission check endpoint
        @PostMapping("/api/users/{userId}/permissions/check")
        ResponseEntity<String> checkPermission(@PathVariable String userId,
                        @RequestBody Object permissionCheckRequest,
                        @RequestHeader("Authorization") String authHeader);
}
