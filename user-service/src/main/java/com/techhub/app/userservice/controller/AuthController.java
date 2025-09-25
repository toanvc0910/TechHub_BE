package com.techhub.app.userservice.controller;

import com.techhub.app.commonservice.payload.GlobalResponse;
import com.techhub.app.userservice.dto.request.CreateUserRequest;
import com.techhub.app.userservice.dto.request.LoginRequest;
import com.techhub.app.userservice.dto.response.AuthResponse;
import com.techhub.app.userservice.dto.response.UserResponse;
import com.techhub.app.userservice.service.AuthService;
import com.techhub.app.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<GlobalResponse<UserResponse>> register(
            @Valid @RequestBody CreateUserRequest request,
            HttpServletRequest httpRequest) {
        try {
            log.info("Register request for email: {}", request.getEmail());
            UserResponse userResponse = userService.createUser(request);

            return ResponseEntity.status(201).body(
                    GlobalResponse.success("User registered successfully", userResponse)
                            .withPath(httpRequest.getRequestURI())
            );
        } catch (Exception e) {
            log.error("Registration failed for email: {}", request.getEmail(), e);
            return ResponseEntity.badRequest().body(
                    GlobalResponse.<UserResponse>error(e.getMessage(), 400)
                            .withPath(httpRequest.getRequestURI())
            );
        }
    }

    @PostMapping("/login")
    public ResponseEntity<GlobalResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        try {
            log.info("Login request for email: {}", request.getEmail());
            AuthResponse authResponse = authService.authenticate(request);

            return ResponseEntity.ok(
                    GlobalResponse.success("Login successful", authResponse)
                            .withPath(httpRequest.getRequestURI())
            );
        } catch (Exception e) {
            log.error("Login failed for email: {}", request.getEmail(), e);
            return ResponseEntity.badRequest().body(
                    GlobalResponse.<AuthResponse>error(e.getMessage(), 400)
                            .withPath(httpRequest.getRequestURI())
            );
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<GlobalResponse<String>> logout(
            @RequestHeader("Authorization") String authHeader,
            HttpServletRequest httpRequest) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                authService.logout(token);

                return ResponseEntity.ok(
                        GlobalResponse.success("Logout successful", "User logged out successfully")
                                .withPath(httpRequest.getRequestURI())
                );
            }
            return ResponseEntity.badRequest().body(
                    GlobalResponse.<String>error("Invalid token format", 400)
                            .withPath(httpRequest.getRequestURI())
            );
        } catch (Exception e) {
            log.error("Logout failed", e);
            return ResponseEntity.badRequest().body(
                    GlobalResponse.<String>error("Logout failed", 400)
                            .withPath(httpRequest.getRequestURI())
            );
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<GlobalResponse<Boolean>> validateToken(
            @RequestHeader("Authorization") String authHeader,
            HttpServletRequest httpRequest) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                boolean isValid = authService.validateToken(token);

                return ResponseEntity.ok(
                        GlobalResponse.success("Token validation completed", isValid)
                                .withPath(httpRequest.getRequestURI())
                );
            }
            return ResponseEntity.badRequest().body(
                    GlobalResponse.<Boolean>error("Invalid token format", 400)
                            .withPath(httpRequest.getRequestURI())
            );
        } catch (Exception e) {
            log.error("Token validation failed", e);
            return ResponseEntity.badRequest().body(
                    GlobalResponse.<Boolean>error("Token validation failed", 400)
                            .withPath(httpRequest.getRequestURI())
            );
        }
    }

    @GetMapping("/health")
    public ResponseEntity<GlobalResponse<String>> health(HttpServletRequest httpRequest) {
        return ResponseEntity.ok(
                GlobalResponse.success("User service is running", "OK")
                        .withPath(httpRequest.getRequestURI())
        );
    }
}
