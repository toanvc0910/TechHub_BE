package com.techhub.app.userservice.service;

import com.techhub.app.commonservice.jwt.JwtUtil;
import com.techhub.app.userservice.dto.request.LoginRequest;
import com.techhub.app.userservice.dto.response.AuthResponse;
import com.techhub.app.userservice.entity.AuthenticationLog;
import com.techhub.app.userservice.entity.User;
import com.techhub.app.userservice.enums.UserStatus;
import com.techhub.app.userservice.repository.AuthenticationLogRepository;
import com.techhub.app.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final AuthenticationLogRepository authLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil; // Add JwtUtil dependency

    /**
     * Authenticate user credentials and generate JWT tokens
     */
    @Transactional
    public AuthResponse authenticate(LoginRequest request) {
        log.info("Authenticating user with email: {}", request.getEmail());

        // Find user by email
        User user = userRepository.findByEmailAndIsActiveTrue(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        // Check if user is active
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new RuntimeException("User account is not active");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            logFailedAuthentication(user, "Invalid password");
            throw new RuntimeException("Invalid credentials");
        }

        // Get user roles from entity-based system
        List<String> roles = user.getUserRoles().stream()
                .map(userRole -> userRole.getRole().getName())
                .collect(Collectors.toList());

        // Log successful authentication
        logSuccessfulAuthentication(user);

        // Generate JWT tokens
        String accessToken = jwtUtil.generateToken(user.getId(), user.getEmail(), roles);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getEmail());

        // Build response WITH JWT tokens
        AuthResponse.UserInfo userInfo = AuthResponse.UserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .roles(roles)
                .status(user.getStatus().name())
                .build();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(86400) // 24 hours in seconds
                .user(userInfo)
                .build();
    }

    /**
     * Logout user - Remove JWT token validation as it should be handled by proxy-client
     */
    @Transactional
    public void logout(String userEmail) {
        try {
            User user = userRepository.findByEmailAndIsActiveTrue(userEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            logLogout(user);
            log.info("User {} logged out successfully", userEmail);
        } catch (Exception e) {
            log.error("Error during logout: {}", e.getMessage());
            throw new RuntimeException("Logout failed");
        }
    }

    /**
     * Remove token validation - this should be handled by proxy-client
     */
    // public boolean validateToken(String token) {
    //     return jwtUtil.validateToken(token);
    // }

    private void logSuccessfulAuthentication(User user) {
        AuthenticationLog authLog = new AuthenticationLog();
        authLog.setUser(user);
        authLog.setLoginTime(LocalDateTime.now());
        authLog.setSuccess(true);
        authLog.setIpAddress(""); // Will be set by controller
        authLog.setDevice(""); // Will be set by controller
        authLog.setIsActive(true);
        authLogRepository.save(authLog);
    }

    private void logFailedAuthentication(User user, String reason) {
        AuthenticationLog authLog = new AuthenticationLog();
        authLog.setUser(user);
        authLog.setLoginTime(LocalDateTime.now());
        authLog.setSuccess(false);
        authLog.setIpAddress(""); // Will be set by controller
        authLog.setDevice(reason); // Store failure reason in device field temporarily
        authLog.setIsActive(true);
        authLogRepository.save(authLog);
    }

    private void logLogout(User user) {
        AuthenticationLog authLog = new AuthenticationLog();
        authLog.setUser(user);
        authLog.setLoginTime(LocalDateTime.now()); // Use loginTime for logout as well
        authLog.setSuccess(true);
        authLog.setIpAddress(""); // Will be set by controller
        authLog.setDevice("logout");
        authLog.setIsActive(true);
        authLogRepository.save(authLog);
    }
}
