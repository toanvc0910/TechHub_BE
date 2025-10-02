package com.techhub.app.proxyclient.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "USER-SERVICE")
public interface UserServiceClient {
    // Auth endpoints
    @PostMapping("/api/auth/register")
    ResponseEntity<String> register(@RequestBody Object registerRequest);

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
}
