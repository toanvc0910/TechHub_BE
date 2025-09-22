package com.techhub.app.proxyclient.controller;

import com.techhub.app.proxyclient.client.UserServiceClient;
import com.techhub.app.proxyclient.client.dto.ApiResponse;
import com.techhub.app.proxyclient.client.dto.CreateUserRequest;
import com.techhub.app.proxyclient.client.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/app/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserProxyController {

    private final UserServiceClient userServiceClient;

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("Proxy: Creating user via gateway -> proxy -> user-service");
        return ResponseEntity.ok(userServiceClient.createUser(request));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID userId) {
        log.info("Proxy: Getting user by ID: {}", userId);
        return ResponseEntity.ok(userServiceClient.getUserById(userId));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserByEmail(@PathVariable String email) {
        log.info("Proxy: Getting user by email: {}", email);
        return ResponseEntity.ok(userServiceClient.getUserByEmail(email));
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserByUsername(@PathVariable String username) {
        log.info("Proxy: Getting user by username: {}", username);
        return ResponseEntity.ok(userServiceClient.getUserByUsername(username));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Proxy: Getting all users with pagination");
        return ResponseEntity.ok(userServiceClient.getAllUsers(page, size));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> searchUsers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Proxy: Searching users with keyword: {}", keyword);
        return ResponseEntity.ok(userServiceClient.searchUsers(keyword, page, size));
    }

    @GetMapping("/exists/email/{email}")
    public ResponseEntity<ApiResponse<Boolean>> checkEmailExists(@PathVariable String email) {
        log.info("Proxy: Checking if email exists: {}", email);
        return ResponseEntity.ok(userServiceClient.checkEmailExists(email));
    }

    @GetMapping("/exists/username/{username}")
    public ResponseEntity<ApiResponse<Boolean>> checkUsernameExists(@PathVariable String username) {
        log.info("Proxy: Checking if username exists: {}", username);
        return ResponseEntity.ok(userServiceClient.checkUsernameExists(username));
    }
}
