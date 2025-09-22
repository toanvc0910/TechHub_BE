package com.techhub.app.proxyclient.controller;

import com.techhub.app.proxyclient.client.dto.ApiResponse;
import com.techhub.app.proxyclient.dto.request.LoginRequest;
import com.techhub.app.proxyclient.dto.response.AuthResponse;
import com.techhub.app.proxyclient.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/app/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        try {
            log.info("Proxy: Login request via gateway -> proxy for email: {}", request.getEmail());
            AuthResponse authResponse = authenticationService.authenticate(request);
            return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
        } catch (Exception e) {
            log.error("Proxy: Login failed for email: {}", request.getEmail(), e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid credentials"));
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<Boolean>> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            log.info("Proxy: Token validation request via gateway -> proxy");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                authenticationService.validateToken(token);
                return ResponseEntity.ok(ApiResponse.success("Token is valid", true));
            }
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid token format"));
        } catch (Exception e) {
            log.error("Proxy: Token validation failed", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid token"));
        }
    }
}
