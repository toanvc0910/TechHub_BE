package com.techhub.app.userservice.controller;

import com.techhub.app.commonservice.exception.BadRequestException;
import com.techhub.app.commonservice.payload.GlobalResponse;
import com.techhub.app.commonservice.payload.PageGlobalResponse;
import com.techhub.app.commonservice.enums.UserRole;
import com.techhub.app.userservice.dto.request.ChangePasswordRequest;
import com.techhub.app.userservice.dto.request.CreateUserRequest;
import com.techhub.app.userservice.dto.request.ForgotPasswordRequest;
import com.techhub.app.userservice.dto.request.ResetPasswordRequest;
import com.techhub.app.userservice.dto.request.UpdateUserRequest;
import com.techhub.app.userservice.dto.response.UserResponse;
import com.techhub.app.userservice.enums.UserStatus;
import com.techhub.app.userservice.service.PermissionService;
import com.techhub.app.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.Email;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final PermissionService permissionService;

    @PostMapping
    public ResponseEntity<GlobalResponse<UserResponse>> createUser(@Valid @RequestBody CreateUserRequest request,
            HttpServletRequest httpServletRequest) {
        UserResponse user = userService.createUser(request);
        return ResponseEntity.status(201)
                .body(GlobalResponse.success("User created successfully", user)
                        .withPath(httpServletRequest.getRequestURI()));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<GlobalResponse<UserResponse>> getUserById(@PathVariable UUID userId,
            HttpServletRequest request) {
        UserResponse userResponse = userService.getUserById(userId);

        return ResponseEntity.ok(
                GlobalResponse.success("User retrieved successfully", userResponse)
                        .withPath(request.getRequestURI()));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<GlobalResponse<UserResponse>> getUserByEmail(@PathVariable @Email String email,
            HttpServletRequest request) {
        UserResponse userResponse = userService.getUserByEmail(email);
        return ResponseEntity.ok(GlobalResponse.success(userResponse)
                .withPath(request.getRequestURI()));
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<GlobalResponse<UserResponse>> getUserByUsername(@PathVariable String username,
            HttpServletRequest request) {
        UserResponse user = userService.getUserByUsername(username);
        return ResponseEntity.ok(GlobalResponse.success(user)
                .withPath(request.getRequestURI()));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<GlobalResponse<UserResponse>> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRequest request, HttpServletRequest httpServletRequest) {
        UserResponse userResponse = userService.updateUser(userId, request);

        return ResponseEntity.ok(
                GlobalResponse.success("User updated successfully", userResponse)
                        .withPath(httpServletRequest.getRequestURI()));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<GlobalResponse<String>> deleteUser(@PathVariable UUID userId,
            HttpServletRequest httpServletRequest) {
        userService.deleteUser(userId);

        return ResponseEntity.ok(
                GlobalResponse.success("User deleted successfully", "User with id " + userId + " has been deleted")
                        .withPath(httpServletRequest.getRequestURI()));
    }

    @PostMapping("/change-password")
    public ResponseEntity<GlobalResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpServletRequest,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        UUID userId = parseRequiredUserId(userIdHeader, "Authentication required - missing X-User-Id header");

        userService.changePassword(userId, request);
        return ResponseEntity.ok(
                GlobalResponse.<Void>success("Password changed successfully", null)
                        .withPath(httpServletRequest.getRequestURI()));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<GlobalResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        userService.forgotPassword(request);
        return ResponseEntity.ok(GlobalResponse.success("Password reset email sent", null));
    }

    @PostMapping("/resend-reset-code/{email}")
    public ResponseEntity<GlobalResponse<String>> resendResetPasswordCode(@PathVariable @Email String email,
            HttpServletRequest httpRequest) {
        userService.resendResetPasswordCode(email);
        return ResponseEntity.ok(
                GlobalResponse.success("Reset password code resent successfully", "Code sent to email")
                        .withPath(httpRequest.getRequestURI()));
    }

    @PostMapping("/reset-password/{email}")
    public ResponseEntity<GlobalResponse<Void>> resetPassword(
            @PathVariable @Email String email,
            @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(email, request);
        return ResponseEntity.ok(GlobalResponse.success("Password reset successfully", null));
    }

    @PostMapping("/{userId}/activate")
    public ResponseEntity<GlobalResponse<Void>> activateUser(@PathVariable UUID userId) {
        userService.activateUser(userId);
        return ResponseEntity.ok(GlobalResponse.success("User activated successfully", null));
    }

    @PostMapping("/{userId}/deactivate")
    public ResponseEntity<GlobalResponse<Void>> deactivateUser(@PathVariable UUID userId) {
        userService.deactivateUser(userId);
        return ResponseEntity.ok(GlobalResponse.success("User deactivated successfully", null));
    }

    @PutMapping("/{userId}/status/{status}")
    public ResponseEntity<GlobalResponse<Void>> changeUserStatus(
            @PathVariable UUID userId,
            @PathVariable UserStatus status) {
        userService.changeUserStatus(userId, status);
        return ResponseEntity.ok(GlobalResponse.success("User status changed successfully", null));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<PageGlobalResponse<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            HttpServletRequest request) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserResponse> userPage;

        if (search != null && !search.trim().isEmpty()) {
            userPage = userService.searchUsers(search.trim(), pageable);
        } else {
            userPage = userService.getAllUsers(pageable);
        }

        PageGlobalResponse.PaginationInfo paginationInfo = PageGlobalResponse.PaginationInfo.builder()
                .page(userPage.getNumber())
                .size(userPage.getSize())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .first(userPage.isFirst())
                .last(userPage.isLast())
                .hasNext(userPage.hasNext())
                .hasPrevious(userPage.hasPrevious())
                .build();

        return ResponseEntity.ok(
                PageGlobalResponse.success("Users retrieved successfully", userPage.getContent(), paginationInfo)
                        .withPath(request.getRequestURI()));
    }

    @GetMapping("/public/instructors")
    public ResponseEntity<PageGlobalResponse<UserResponse>> getPublicInstructors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "4") int size,
            HttpServletRequest request) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserResponse> instructorPage = userService.getInstructorsByRole(UserRole.INSTRUCTOR.name(), pageable);

        PageGlobalResponse.PaginationInfo paginationInfo = PageGlobalResponse.PaginationInfo.builder()
                .page(instructorPage.getNumber())
                .size(instructorPage.getSize())
                .totalElements(instructorPage.getTotalElements())
                .totalPages(instructorPage.getTotalPages())
                .first(instructorPage.isFirst())
                .last(instructorPage.isLast())
                .hasNext(instructorPage.hasNext())
                .hasPrevious(instructorPage.hasPrevious())
                .build();

        return ResponseEntity.ok(
                PageGlobalResponse
                        .success("Instructors retrieved successfully", instructorPage.getContent(), paginationInfo)
                        .withPath(request.getRequestURI()));
    }

    @GetMapping("/profile")
    public ResponseEntity<GlobalResponse<UserResponse>> getCurrentUserProfile(
            HttpServletRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        UUID userId = parseUserIdForProfile(userIdHeader, authHeader);

        UserResponse userResponse = userService.getUserById(userId);

        try {
            log.info("🔍 [UserController] Getting effective permissions for user: {}", userId);
            var permissions = permissionService.getEffectivePermissions(userId);
            log.info("📋 [UserController] User {} has {} effective permissions:", userId, permissions.size());
            permissions.forEach(p -> {
                log.info("   ✓ {} {} - {} (source: {}, allowed: {})",
                        p.getMethod(), p.getUrl(), p.getName(), p.getSource(), p.getAllowed());
            });
        } catch (RuntimeException e) {
            log.warn("⚠️ [UserController] Failed to log user permissions: {}", e.getMessage());
        }

        return ResponseEntity.ok(
                GlobalResponse.success("Profile retrieved successfully", userResponse)
                        .withPath(request.getRequestURI()));
    }

    @PutMapping("/profile")
    public ResponseEntity<GlobalResponse<UserResponse>> updateCurrentUserProfile(
            @Valid @RequestBody UpdateUserRequest request,
            HttpServletRequest httpServletRequest,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        UUID userId = parseRequiredUserId(userIdHeader, "Authentication required - missing X-User-Id header");

        UserResponse userResponse = userService.updateUser(userId, request);

        return ResponseEntity.ok(
                GlobalResponse.success("Profile updated successfully", userResponse)
                        .withPath(httpServletRequest.getRequestURI()));
    }

    /**
     * Internal endpoint for getting all active user IDs (for broadcast
     * notifications)
     * This endpoint is called by other services, not exposed to public API
     */
    @GetMapping("/internal/all-user-ids")
    public ResponseEntity<GlobalResponse<java.util.List<UUID>>> getAllActiveUserIds(
            HttpServletRequest httpServletRequest) {
        java.util.List<UUID> userIds = userService.getAllActiveUserIds();
        log.info("Returning {} active user IDs for broadcast", userIds.size());
        return ResponseEntity.ok(
                GlobalResponse.success("Active user IDs retrieved successfully", userIds)
                        .withPath(httpServletRequest.getRequestURI()));
    }

    private UUID parseRequiredUserId(String userIdHeader, String missingHeaderMessage) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new BadRequestException(missingHeaderMessage);
        }
        try {
            return UUID.fromString(userIdHeader);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Invalid user ID format");
        }
    }

    private UUID parseUserIdForProfile(String userIdHeader, String authHeader) {
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            return parseRequiredUserId(userIdHeader, "Authentication required - missing X-User-Id header");
        }

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            throw new BadRequestException(
                    "Profile access requires going through Proxy-Client or provide X-User-Id header");
        }

        throw new BadRequestException("Authentication required - missing X-User-Id header or Authorization token");
    }
}
