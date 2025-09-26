package com.techhub.app.proxyclient.controller;

import com.techhub.app.proxyclient.client.UserServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/proxy/auth")
@RequiredArgsConstructor
public class AuthProxyController {
    private final UserServiceClient userServiceClient;
    // Auth-related endpoints
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody Object registerRequest) {
        return userServiceClient.register(registerRequest);
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody Object loginRequest) {
        return userServiceClient.login(loginRequest);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String authHeader) {
        return userServiceClient.logout(authHeader);
    }

    @PostMapping("/validate")
    public ResponseEntity<String> validateToken(@RequestHeader("Authorization") String authHeader) {
        return userServiceClient.validateToken(authHeader);
    }

    @GetMapping("/health")
    public ResponseEntity<String> authHealth() {
        return userServiceClient.authHealth();
    }
}
