package com.techhub.app.userservice.controller;

import com.techhub.app.commonservice.payload.GlobalResponse;
import com.techhub.app.commonservice.payload.PageGlobalResponse;
import com.techhub.app.userservice.dto.request.ChangePasswordRequest;
import com.techhub.app.userservice.dto.request.CreateUserRequest;
import com.techhub.app.userservice.dto.request.ForgotPasswordRequest;
import com.techhub.app.userservice.dto.request.ResetPasswordRequest;
import com.techhub.app.userservice.dto.request.UpdateUserRequest;
import com.techhub.app.userservice.dto.response.UserResponse;
import com.techhub.app.userservice.enums.UserStatus;
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

    @PostMapping
    public ResponseEntity<GlobalResponse<UserResponse>> createUser(@Valid @RequestBody CreateUserRequest request, HttpServletRequest httpServletRequest) {
        try {
            UserResponse user = userService.createUser(request);
            return ResponseEntity.status(201)
                .body(GlobalResponse.success("User created successfully", user)
                    .withPath(httpServletRequest.getRequestURI()));
        } catch (Exception e) {
            log.error("Error creating user", e);
            return ResponseEntity.badRequest()
                .body(GlobalResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<GlobalResponse<UserResponse>> getUserById(@PathVariable UUID userId, HttpServletRequest request) {
        try {
            UserResponse userResponse = userService.getUserById(userId);

            return ResponseEntity.ok(
                GlobalResponse.success("User retrieved successfully", userResponse)
                    .withPath(request.getRequestURI())
            );

        } catch (Exception e) {
            log.error("Error retrieving user with id: {}", userId, e);
            return ResponseEntity.badRequest().body(
                GlobalResponse.<UserResponse>error("User not found", 404)
                    .withPath(request.getRequestURI())
            );
        }
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<GlobalResponse<UserResponse>> getUserByEmail(@PathVariable @Email String email, HttpServletRequest request) {
        try {
            UserResponse userResponse = userService.getUserByEmail(email);
            return ResponseEntity.ok(GlobalResponse.success(userResponse)
                    .withPath(request.getRequestURI()));
        } catch (Exception e) {
            log.error("Error getting user by email: {}", email, e);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<GlobalResponse<UserResponse>> getUserByUsername(@PathVariable String username, HttpServletRequest request) {
        try {
            UserResponse user = userService.getUserByUsername(username);
            return ResponseEntity.ok(GlobalResponse.success(user)
                    .withPath(request.getRequestURI()));
        } catch (Exception e) {
            log.error("Error getting user by username: {}", username, e);
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{userId}")
    public ResponseEntity<GlobalResponse<UserResponse>> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRequest request, HttpServletRequest httpServletRequest) {
        try {
            UserResponse userResponse = userService.updateUser(userId, request);

            return ResponseEntity.ok(
                GlobalResponse.success("User updated successfully", userResponse)
                    .withPath(httpServletRequest.getRequestURI())
            );

        } catch (Exception e) {
            log.error("Error updating user with id: {}", userId, e);
            return ResponseEntity.badRequest().body(
                GlobalResponse.<UserResponse>error(e.getMessage(), 400)
                    .withPath(httpServletRequest.getRequestURI())
            );
        }
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<GlobalResponse<String>> deleteUser(@PathVariable UUID userId, HttpServletRequest httpServletRequest) {
        try {
            userService.deleteUser(userId);

            return ResponseEntity.ok(
                GlobalResponse.success("User deleted successfully", "User with id " + userId + " has been deleted")
                    .withPath(httpServletRequest.getRequestURI())
            );

        } catch (Exception e) {
            log.error("Error deleting user with id: {}", userId, e);
            return ResponseEntity.badRequest().body(
                GlobalResponse.<String>error(e.getMessage(), 400)
                    .withPath(httpServletRequest.getRequestURI())
            );
        }
    }

    @PostMapping("/{userId}/change-password")
    public ResponseEntity<GlobalResponse<Void>> changePassword(
            @PathVariable UUID userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        try {
            userService.changePassword(userId, request);
            return ResponseEntity.ok(GlobalResponse.success("Password changed successfully", null));
        } catch (Exception e) {
            log.error("Error changing password for user: {}", userId, e);
            return ResponseEntity.badRequest()
                .body(GlobalResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<GlobalResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            userService.forgotPassword(request);
            return ResponseEntity.ok(GlobalResponse.success("Password reset email sent", null));
        } catch (Exception e) {
            log.error("Error processing forgot password request", e);
            return ResponseEntity.badRequest()
                .body(GlobalResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/reset-password/{email}")
    public ResponseEntity<GlobalResponse<Void>> resetPassword(
            @PathVariable @Email String email,
            @Valid @RequestBody ResetPasswordRequest request) {
        try {
            userService.resetPassword(email, request);
            return ResponseEntity.ok(GlobalResponse.success("Password reset successfully", null));
        } catch (Exception e) {
            log.error("Error resetting password for email: {}", email, e);
            return ResponseEntity.badRequest()
                .body(GlobalResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{userId}/activate")
    public ResponseEntity<GlobalResponse<Void>> activateUser(@PathVariable UUID userId) {
        try {
            userService.activateUser(userId);
            return ResponseEntity.ok(GlobalResponse.success("User activated successfully", null));
        } catch (Exception e) {
            log.error("Error activating user: {}", userId, e);
            return ResponseEntity.badRequest()
                .body(GlobalResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{userId}/deactivate")
    public ResponseEntity<GlobalResponse<Void>> deactivateUser(@PathVariable UUID userId) {
        try {
            userService.deactivateUser(userId);
            return ResponseEntity.ok(GlobalResponse.success("User deactivated successfully", null));
        } catch (Exception e) {
            log.error("Error deactivating user: {}", userId, e);
            return ResponseEntity.badRequest()
                .body(GlobalResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{userId}/status/{status}")
    public ResponseEntity<GlobalResponse<Void>> changeUserStatus(
            @PathVariable UUID userId,
            @PathVariable UserStatus status) {
        try {
            userService.changeUserStatus(userId, status);
            return ResponseEntity.ok(GlobalResponse.success("User status changed successfully", null));
        } catch (Exception e) {
            log.error("Error changing user status: {}", userId, e);
            return ResponseEntity.badRequest()
                .body(GlobalResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<PageGlobalResponse<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            HttpServletRequest request) {

        try {
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
                    .withPath(request.getRequestURI())
            );

        } catch (Exception e) {
            log.error("Error retrieving users", e);
            return ResponseEntity.badRequest().body(
                PageGlobalResponse.<UserResponse>error("Failed to retrieve users")
                    .withPath(request.getRequestURI())
            );
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<GlobalResponse<UserResponse>> getCurrentUserProfile(
            HttpServletRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            UUID userId = null;

            // Ưu tiên lấy từ header X-User-Id (từ Proxy-Client)
            if (userIdHeader != null && !userIdHeader.isEmpty()) {
                userId = UUID.fromString(userIdHeader);
            }
            // Fallback: Lấy từ JWT token nếu không có header
            else if (authHeader != null && authHeader.startsWith("Bearer ")) {
                // TODO: Extract user ID from JWT token if needed
                return ResponseEntity.badRequest().body(
                    GlobalResponse.<UserResponse>error("Profile access requires going through Proxy-Client or provide X-User-Id header", 400)
                        .withPath(request.getRequestURI())
                );
            } else {
                return ResponseEntity.badRequest().body(
                    GlobalResponse.<UserResponse>error("Authentication required - missing X-User-Id header or Authorization token", 400)
                        .withPath(request.getRequestURI())
                );
            }

            UserResponse userResponse = userService.getUserById(userId);

            return ResponseEntity.ok(
                GlobalResponse.success("Profile retrieved successfully", userResponse)
                    .withPath(request.getRequestURI())
            );

        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format in X-User-Id header: {}", userIdHeader, e);
            return ResponseEntity.badRequest().body(
                GlobalResponse.<UserResponse>error("Invalid user ID format", 400)
                    .withPath(request.getRequestURI())
            );
        } catch (Exception e) {
            log.error("Error retrieving current user profile", e);
            return ResponseEntity.badRequest().body(
                GlobalResponse.<UserResponse>error("Failed to retrieve profile: " + e.getMessage(), 400)
                    .withPath(request.getRequestURI())
            );
        }
    }
}
