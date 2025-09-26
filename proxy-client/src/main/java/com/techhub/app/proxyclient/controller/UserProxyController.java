package com.techhub.app.proxyclient.controller;

import com.techhub.app.proxyclient.client.UserServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/proxy/users")
@RequiredArgsConstructor
public class UserProxyController {

    private final UserServiceClient userServiceClient;

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
    @PostMapping("/{userId}/change-password")
    public ResponseEntity<String> changePassword(@PathVariable String userId,
                                                @RequestBody Object changePasswordRequest,
                                                @RequestHeader("Authorization") String authHeader) {
        return userServiceClient.changePassword(userId, changePasswordRequest, authHeader);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody Object forgotPasswordRequest) {
        return userServiceClient.forgotPassword(forgotPasswordRequest);
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
    public ResponseEntity<String> getCurrentUserProfile(@RequestHeader("Authorization") String authHeader) {
        return userServiceClient.getCurrentUserProfile(authHeader);
    }
}
