package com.techhub.app.proxyclient.controller;

import com.techhub.app.proxyclient.client.UserServiceClient;
import com.techhub.app.commonservice.jwt.JwtUtil;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("/api/proxy/users")
@RequiredArgsConstructor
public class UserProxyController {

    private final UserServiceClient userServiceClient;
    private final JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<String> getAllUsers(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestHeader("Authorization") String authHeader) {
        return userServiceClient.getAllUsers(page, size, search, authHeader);
    }

    @PostMapping
    public ResponseEntity<String> createUser(@RequestBody Object createUserRequest) {
        return userServiceClient.createUser(createUserRequest);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<String> getUserById(@PathVariable String userId,
            @RequestHeader("Authorization") String authHeader) {
        return userServiceClient.getUserById(userId, authHeader);
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<String> getUserByEmail(@PathVariable String email,
            @RequestHeader("Authorization") String authHeader) {
        return userServiceClient.getUserByEmail(email, authHeader);
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<String> getUserByUsername(@PathVariable String username,
            @RequestHeader("Authorization") String authHeader) {
        return userServiceClient.getUserByUsername(username, authHeader);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<String> updateUser(@PathVariable String userId,
            @RequestBody Object updateRequest,
            @RequestHeader("Authorization") String authHeader) {
        return userServiceClient.updateUser(userId, updateRequest, authHeader);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<String> deleteUser(@PathVariable String userId,
            @RequestHeader("Authorization") String authHeader) {
        return userServiceClient.deleteUser(userId, authHeader);
    }

    // Password management endpoints
    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @RequestBody Object changePasswordRequest,
            @RequestHeader("Authorization") String authHeader) {
        try {
            // Extract userId from JWT token
            String token = authHeader.replace("Bearer ", "");
            UUID userId = jwtUtil.getUserIdFromToken(token);

            log.info("Change password request for user: {}", userId);
            return userServiceClient.changePassword(changePasswordRequest, userId.toString());
        } catch (Exception e) {
            log.error("Error processing change password request", e);
            return ResponseEntity.badRequest().body(
                    String.format("{\"success\":false,\"message\":\"Failed to process change password request: %s\"}",
                            e.getMessage()));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody Object forgotPasswordRequest) {
        return userServiceClient.forgotPassword(forgotPasswordRequest);
    }

    @PostMapping("/resend-reset-code/{email}")
    public ResponseEntity<String> resendResetCode(@PathVariable String email) {
        return userServiceClient.resendResetCode(email);
    }

    @PostMapping("/reset-password/{email}")
    public ResponseEntity<String> resetPassword(@PathVariable String email,
            @RequestBody Object resetPasswordRequest) {
        return userServiceClient.resetPassword(email, resetPasswordRequest);
    }

    // User status management endpoints
    @PostMapping("/{userId}/activate")
    public ResponseEntity<String> activateUser(@PathVariable String userId,
            @RequestHeader("Authorization") String authHeader) {
        return userServiceClient.activateUser(userId, authHeader);
    }

    @PostMapping("/{userId}/deactivate")
    public ResponseEntity<String> deactivateUser(@PathVariable String userId,
            @RequestHeader("Authorization") String authHeader) {
        return userServiceClient.deactivateUser(userId, authHeader);
    }

    @PutMapping("/{userId}/status/{status}")
    public ResponseEntity<String> changeUserStatus(@PathVariable String userId,
            @PathVariable String status,
            @RequestHeader("Authorization") String authHeader) {
        return userServiceClient.changeUserStatus(userId, status, authHeader);
    }

    // Profile endpoint
    @GetMapping("/profile")
    public ResponseEntity<String> getCurrentUserProfile(@RequestHeader("Authorization") String authHeader,
            HttpServletRequest request) {
        // Get user info from request attributes (set by JwtAuthenticationFilter)
        Object userId = request.getAttribute("userId");
        Object userEmail = request.getAttribute("userEmail");

        if (userId == null || userEmail == null) {
            return ResponseEntity.badRequest().body("{\"error\": \"User context missing\"}");
        }

        return userServiceClient.getCurrentUserProfile(authHeader, userId.toString(), userEmail.toString());
    }

    // Public endpoints - no authentication required
    @GetMapping("/public/instructors")
    public ResponseEntity<String> getPublicInstructors(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "4") int size) {
        return userServiceClient.getPublicInstructors(page, size);
    }
}
