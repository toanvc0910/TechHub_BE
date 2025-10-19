package com.techhub.app.userservice.controller;

import com.techhub.app.commonservice.exception.BadRequestException;
import com.techhub.app.commonservice.payload.GlobalResponse;
import com.techhub.app.userservice.dto.request.CreateUserRequest;
import com.techhub.app.userservice.dto.request.LoginRequest;
import com.techhub.app.userservice.dto.request.VerifyEmailRequest;
import com.techhub.app.userservice.dto.response.AuthResponse;
import com.techhub.app.userservice.dto.response.UserResponse;
import com.techhub.app.userservice.service.AuthService;
import com.techhub.app.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<GlobalResponse<UserResponse>> register(@Valid @RequestBody CreateUserRequest request,
                                                                 HttpServletRequest httpRequest) {
        log.info("Register request for email: {}", request.getEmail());
        UserResponse userResponse = userService.registerUser(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalResponse.success("Registration initiated. Please verify your email.", userResponse)
                        .withStatus("REGISTER_PENDING")
                        .withPath(httpRequest.getRequestURI()));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<GlobalResponse<UserResponse>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request,
                                                                    HttpServletRequest httpRequest) {
        log.info("Email verification requested for {}", request.getEmail());
        UserResponse userResponse = userService.verifyUserRegistration(request);

        return ResponseEntity.ok(
                GlobalResponse.success("Email verified successfully", userResponse)
                        .withStatus("REGISTER_VERIFIED")
                        .withPath(httpRequest.getRequestURI()));
    }

    @PostMapping("/login")
    public ResponseEntity<GlobalResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request,
                                                              HttpServletRequest httpRequest) {
        log.info("Login request for email: {}", request.getEmail());
        AuthResponse authResponse = authService.authenticate(request);

        return ResponseEntity.ok(
                GlobalResponse.success("Login successful", authResponse)
                        .withPath(httpRequest.getRequestURI()));
    }

    @PostMapping("/logout")
    public ResponseEntity<GlobalResponse<String>> logout(@RequestHeader("Authorization") String authHeader,
                                                         HttpServletRequest httpRequest) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BadRequestException("Authorization header must contain a Bearer token");
        }

        String token = authHeader.substring(7);
        authService.logout(token);

        return ResponseEntity.ok(
                GlobalResponse.success("Logout successful", "User logged out successfully")
                        .withPath(httpRequest.getRequestURI()));
    }

    @GetMapping("/health")
    public ResponseEntity<GlobalResponse<String>> health(HttpServletRequest httpRequest) {
        return ResponseEntity.ok(
                GlobalResponse.success("User service is running", "OK")
                        .withPath(httpRequest.getRequestURI()));
    }
}
