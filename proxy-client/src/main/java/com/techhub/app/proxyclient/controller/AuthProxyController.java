package com.techhub.app.proxyclient.controller;

import com.techhub.app.proxyclient.client.UserServiceClient;
import com.techhub.app.commonservice.jwt.JwtUtil;
import java.util.Collections;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/proxy/auth")
@RequiredArgsConstructor
public class AuthProxyController {
    private final UserServiceClient userServiceClient;
    private final JwtUtil jwtUtil;
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

    // Exchange OAuth2 result (from FE) to JWT issued by proxy-client
    @PostMapping("/oauth2/exchange")
    public ResponseEntity<Map<String, Object>> oauth2Exchange(@RequestBody Map<String, Object> payload) {
        // Expected payload: { userId: string-uuid, email: string }
        try {
            Object userIdObj = payload.get("userId");
            Object emailObj = payload.get("email");

            if (userIdObj == null || emailObj == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "userId and email are required"
                ));
            }

            java.util.UUID userId = java.util.UUID.fromString(userIdObj.toString());
            String email = emailObj.toString();

            // Issue access token with empty roles for now (roles are enforced in services via headers)
            String accessToken = jwtUtil.generateToken(userId, email, Collections.emptyList());
            String refreshToken = jwtUtil.generateRefreshToken(userId, email);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "tokenType", "Bearer",
                    "accessToken", accessToken,
                    "refreshToken", refreshToken,
                    "expiresIn", 86400
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to exchange OAuth2 result: " + e.getMessage()
            ));
        }
    }
}
