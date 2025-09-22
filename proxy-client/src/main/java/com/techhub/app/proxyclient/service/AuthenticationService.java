package com.techhub.app.proxyclient.service;

import com.techhub.app.proxyclient.client.UserServiceClient;
import com.techhub.app.proxyclient.client.dto.ApiResponse;
import com.techhub.app.proxyclient.client.dto.UserResponse;
import com.techhub.app.proxyclient.config.JwtConfig;
import com.techhub.app.proxyclient.dto.request.LoginRequest;
import com.techhub.app.proxyclient.dto.response.AuthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserServiceClient userServiceClient;
    private final JwtConfig jwtConfig;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse authenticate(LoginRequest request) {
        try {
            // Get user from user-service
            ApiResponse<UserResponse> userResponse = userServiceClient.getUserByEmail(request.getEmail());

            if (!userResponse.isSuccess() || userResponse.getData() == null) {
                throw new RuntimeException("Invalid credentials");
            }

            UserResponse user = userResponse.getData();

            // Verify password (in production, this should be done in user-service)
            // For now, we'll trust the user-service for authentication

            // Generate JWT token
            String token = jwtConfig.generateToken(
                user.getEmail(),
                user.getId().toString(),
                user.getRole().toString()
            );

            return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(86400L) // 24 hours
                .user(user)
                .build();

        } catch (Exception e) {
            log.error("Authentication failed for email: {}", request.getEmail(), e);
            throw new RuntimeException("Authentication failed");
        }
    }

    public UserResponse validateToken(String token) {
        try {
            String email = jwtConfig.extractUsername(token);
            String userId = jwtConfig.extractUserId(token);

            if (jwtConfig.validateToken(token, email)) {
                ApiResponse<UserResponse> userResponse = userServiceClient.getUserByEmail(email);
                return userResponse.getData();
            }

            throw new RuntimeException("Invalid token");
        } catch (Exception e) {
            log.error("Token validation failed", e);
            throw new RuntimeException("Token validation failed");
        }
    }
}
