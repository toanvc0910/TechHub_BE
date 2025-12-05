package com.techhub.app.userservice.service.impl;

import com.techhub.app.commonservice.exception.BadRequestException;
import com.techhub.app.commonservice.exception.ForbiddenException;
import com.techhub.app.commonservice.exception.NotFoundException;
import com.techhub.app.commonservice.exception.UnauthorizedException;
import com.techhub.app.commonservice.jwt.JwtUtil;
import com.techhub.app.userservice.dto.request.LoginRequest;
import com.techhub.app.userservice.dto.request.RefreshTokenRequest;
import com.techhub.app.userservice.dto.request.SaveRefreshTokenRequest;
import com.techhub.app.userservice.dto.response.AuthResponse;
import com.techhub.app.userservice.entity.AuthProvider;
import com.techhub.app.userservice.entity.AuthenticationLog;
import com.techhub.app.userservice.entity.User;
import com.techhub.app.userservice.entity.UserRole;
import com.techhub.app.userservice.enums.AuthProviderEnum;
import com.techhub.app.userservice.enums.UserStatus;
import com.techhub.app.userservice.repository.AuthProviderRepository;
import com.techhub.app.userservice.repository.AuthenticationLogRepository;
import com.techhub.app.userservice.repository.UserRepository;
import com.techhub.app.userservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final AuthenticationLogRepository authenticationLogRepository;
    private final AuthProviderRepository authProviderRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public AuthResponse authenticate(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        log.info("Authenticating user {}", email);

        User user = userRepository.findByEmail(email)
                .filter(User::getIsActive)
                .orElseThrow(() -> {
                    log.warn("Authentication failed. Unknown email {}", email);
                    return new UnauthorizedException("Invalid credentials");
                });

        if (user.getStatus() != UserStatus.ACTIVE) {
            logFailedAuthentication(user, "ACCOUNT_INACTIVE");
            throw new ForbiddenException("Account is not verified or has been disabled");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            logFailedAuthentication(user, "INVALID_PASSWORD");
            throw new UnauthorizedException("Invalid credentials");
        }

        // Ensure roles are initialized before mapping to DTO to avoid lazy proxy issues
        user.getUserRoles().forEach(ur -> {
            if (ur.getIsActive() != null && ur.getIsActive() && ur.getRole() != null) {
                ur.getRole().getName();
            }
        });

        // Mark login type for auditing/debug (local login)
        user.setLoginType("LOCAL");

        List<String> roles = resolveRoles(user);
        logSuccessfulAuthentication(user);

        String accessToken = jwtUtil.generateToken(user.getId(), user.getEmail(), roles);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getEmail());

        // Save refresh token to auth_providers with LOCAL provider
        saveRefreshTokenToDatabase(user.getId(), AuthProviderEnum.LOCAL, refreshToken);

        AuthResponse.UserInfo userInfo = AuthResponse.UserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .roles(roles)
                .status(user.getStatus().name())
                .build();

        return AuthResponse.builder()
                .tokenType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(86400)
                .user(userInfo)
                .build();
    }

    @Override
    @Transactional
    public void logout(String token) {
        if (token == null || token.isBlank()) {
            throw new BadRequestException("Missing authorization token");
        }

        if (!jwtUtil.validateToken(token)) {
            throw new UnauthorizedException("Invalid or expired token");
        }

        UUID userId = jwtUtil.getUserIdFromToken(token);
        User user = userRepository.findByIdAndIsActiveTrue(userId)
                .orElseThrow(() -> new NotFoundException("User not found for logout"));

        logLogout(user);
        log.info("User {} successfully logged out", user.getEmail());
    }

    private List<String> resolveRoles(User user) {
        List<String> roles = user.getUserRoles().stream()
                .filter(UserRole::getIsActive)
                .map(userRole -> userRole.getRole().getName())
                .collect(Collectors.toList());

        return roles;
    }

    private void logSuccessfulAuthentication(User user) {
        AuthenticationLog logEntry = new AuthenticationLog();
        logEntry.setUser(user);
        logEntry.setLoginTime(LocalDateTime.now());
        logEntry.setSuccess(true);
        logEntry.setIpAddress("");
        logEntry.setDevice("login");
        logEntry.setIsActive(true);
        authenticationLogRepository.save(logEntry);
    }

    private void logFailedAuthentication(User user, String reason) {
        AuthenticationLog logEntry = new AuthenticationLog();
        logEntry.setUser(user);
        logEntry.setLoginTime(LocalDateTime.now());
        logEntry.setSuccess(false);
        logEntry.setIpAddress("");
        logEntry.setDevice(reason);
        logEntry.setIsActive(true);
        authenticationLogRepository.save(logEntry);
    }

    private void logLogout(User user) {
        AuthenticationLog logEntry = new AuthenticationLog();
        logEntry.setUser(user);
        logEntry.setLoginTime(LocalDateTime.now());
        logEntry.setSuccess(true);
        logEntry.setIpAddress("");
        logEntry.setDevice("logout");
        logEntry.setIsActive(true);
        authenticationLogRepository.save(logEntry);
    }

    private void saveRefreshTokenToDatabase(UUID userId, AuthProviderEnum provider, String refreshToken) {
        LocalDateTime expiresAt = jwtUtil.getExpirationDateFromRefreshToken(refreshToken);
        
        AuthProvider authProvider = authProviderRepository
                .findByUserIdAndProvider(userId, provider)
                .orElse(new AuthProvider());
        
        authProvider.setUserId(userId);
        authProvider.setProvider(provider);
        authProvider.setRefreshToken(refreshToken);
        authProvider.setExpiresAt(expiresAt);
        authProvider.setAccessToken(null); // Do not save access token
        authProvider.setIsActive(true);
        
        authProviderRepository.save(authProvider);
        log.info("Saved refresh token for user {} with provider {}", userId, provider);
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        
        // Validate JWT format and type
        if (!jwtUtil.validateRefreshToken(refreshToken)) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        // Find refresh token in database
        AuthProvider authProvider = authProviderRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new UnauthorizedException("Refresh token not found in database"));

        // Validate token is still active and not expired
        if (!authProvider.getIsActive()) {
            throw new UnauthorizedException("Refresh token has been revoked");
        }

        if (authProvider.getExpiresAt() != null && authProvider.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Refresh token has expired");
        }

        // Get user info
        UUID userId = jwtUtil.getUserIdFromToken(refreshToken);
        User user = userRepository.findByIdAndIsActiveTrue(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ForbiddenException("Account is not active");
        }

        // Ensure roles are loaded
        user.getUserRoles().forEach(ur -> {
            if (ur.getIsActive() != null && ur.getIsActive() && ur.getRole() != null) {
                ur.getRole().getName();
            }
        });

        List<String> roles = resolveRoles(user);

        // Revoke old refresh token (token rotation)
        authProvider.setRefreshToken(null);
        authProviderRepository.save(authProvider);

        // Generate new tokens
        String newAccessToken = jwtUtil.generateToken(user.getId(), user.getEmail(), roles);
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getEmail());

        // Save new refresh token
        saveRefreshTokenToDatabase(user.getId(), authProvider.getProvider(), newRefreshToken);

        AuthResponse.UserInfo userInfo = AuthResponse.UserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .roles(roles)
                .status(user.getStatus().name())
                .build();

        log.info("Successfully refreshed token for user {}", user.getEmail());

        return AuthResponse.builder()
                .tokenType("Bearer")
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(86400)
                .user(userInfo)
                .build();
    }

    @Override
    @Transactional
    public void saveRefreshToken(SaveRefreshTokenRequest request) {
        AuthProvider authProvider = authProviderRepository
                .findByUserIdAndProvider(request.getUserId(), request.getProvider())
                .orElse(new AuthProvider());
        
        authProvider.setUserId(request.getUserId());
        authProvider.setProvider(request.getProvider());
        authProvider.setRefreshToken(request.getRefreshToken());
        authProvider.setExpiresAt(request.getExpiresAt());
        authProvider.setAccessToken(null); // Do not save access token
        authProvider.setIsActive(true);
        
        authProviderRepository.save(authProvider);
        log.info("Saved refresh token for user {} with provider {}", request.getUserId(), request.getProvider());
    }
}
