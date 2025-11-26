package com.techhub.app.proxyclient.controller;

import com.techhub.app.proxyclient.client.UserServiceClient;
import com.techhub.app.commonservice.jwt.JwtUtil;
import java.util.Collections;
import java.util.Map;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/proxy/auth")
@RequiredArgsConstructor
public class AuthProxyController {
    private final UserServiceClient userServiceClient;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    // Auth-related endpoints
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody Object registerRequest) {
        return userServiceClient.register(registerRequest);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@RequestBody Object verifyEmailRequest) {
        return userServiceClient.verifyEmail(verifyEmailRequest);
    }

    @PostMapping("/resend-code")
    public ResponseEntity<String> resendCode(@RequestBody Object resendCodeRequest) {
        return userServiceClient.resendCode(resendCodeRequest);
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
                        "message", "userId and email are required"));
            }

            java.util.UUID userId = java.util.UUID.fromString(userIdObj.toString());
            String email = emailObj.toString();

            log.info("OAuth2 exchange - userId: {}, email: {}", userId, email);

            // Fetch full user info (roles, username) from user-service
            // Add retry logic for newly created OAuth2 users (transaction may not be
            // committed yet)
            ResponseEntity<String> userResponse = null;
            int maxRetries = 3;

            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    log.info("Fetching user from user-service (attempt {}/{})", attempt, maxRetries);
                    userResponse = userServiceClient.getUserByIdInternal(
                            userId.toString(),
                            "proxy-client",
                            userId.toString(),
                            email,
                            "");

                    if (userResponse.getStatusCode().is2xxSuccessful() && userResponse.getBody() != null) {
                        log.info("Successfully fetched user from user-service");
                        break;
                    } else {
                        log.warn("User fetch returned status: {}", userResponse.getStatusCode());
                    }
                } catch (Exception e) {
                    log.warn("Failed to fetch user (attempt {}/{}): {}", attempt, maxRetries, e.getMessage());
                    if (attempt < maxRetries) {
                        Thread.sleep(500); // Wait 500ms before retry
                    } else {
                        log.error("All retry attempts exhausted", e);
                        throw e;
                    }
                }
            }

            if (userResponse == null || !userResponse.getStatusCode().is2xxSuccessful()
                    || userResponse.getBody() == null) {
                log.error("Failed to fetch user info after {} retries", maxRetries);
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Failed to fetch user info for OAuth2 exchange"));
            }

            log.info("Parsing user response");
            JsonNode root = objectMapper.readTree(userResponse.getBody());
            JsonNode dataNode = root.path("data");
            if (!root.path("success").asBoolean(false) || dataNode.isMissingNode()) {
                log.error("Invalid user response - success: {}, data present: {}",
                        root.path("success").asBoolean(false), !dataNode.isMissingNode());
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "User info not available for OAuth2 exchange"));
            }

            List<String> roles = objectMapper.convertValue(
                    dataNode.path("roles"),
                    new TypeReference<List<String>>() {
                    });
            if (roles == null) {
                roles = Collections.emptyList();
            }

            // Issue access/refresh tokens with resolved roles
            String accessToken = jwtUtil.generateToken(userId, email, roles);
            String refreshToken = jwtUtil.generateRefreshToken(userId, email);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "tokenType", "Bearer",
                    "accessToken", accessToken,
                    "refreshToken", refreshToken,
                    "expiresIn", 86400,
                    "user", Map.of(
                            "id", dataNode.path("id").asText(userId.toString()),
                            "email", dataNode.path("email").asText(email),
                            "username", dataNode.path("username").asText(""),
                            "roles", roles,
                            "status", dataNode.path("status").asText("ACTIVE"))));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to exchange OAuth2 result: " + e.getMessage()));
        }
    }
}
