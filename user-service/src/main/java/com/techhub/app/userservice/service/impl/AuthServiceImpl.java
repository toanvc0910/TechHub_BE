package com.techhub.app.userservice.service.impl;

import com.techhub.app.commonservice.exception.BadRequestException;
import com.techhub.app.commonservice.exception.ForbiddenException;
import com.techhub.app.commonservice.exception.NotFoundException;
import com.techhub.app.commonservice.exception.UnauthorizedException;
import com.techhub.app.commonservice.jwt.JwtUtil;
import com.techhub.app.userservice.dto.request.LoginRequest;
import com.techhub.app.userservice.dto.response.AuthResponse;
import com.techhub.app.userservice.entity.AuthenticationLog;
import com.techhub.app.userservice.entity.User;
import com.techhub.app.userservice.entity.UserRole;
import com.techhub.app.userservice.enums.UserStatus;
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

        List<String> roles = resolveRoles(user);
        logSuccessfulAuthentication(user);

        String accessToken = jwtUtil.generateToken(user.getId(), user.getEmail(), roles);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getEmail());

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

        if (roles.isEmpty()) {
            roles = List.of(user.getRole().name());
        }
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
}
