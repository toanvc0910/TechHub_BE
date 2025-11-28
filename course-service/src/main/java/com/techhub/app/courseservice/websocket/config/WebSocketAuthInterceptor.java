package com.techhub.app.courseservice.websocket.config;

import com.techhub.app.commonservice.jwt.JwtUtil;
import com.techhub.app.commonservice.websocket.interceptor.BaseWebSocketAuthInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * WebSocket Authentication Interceptor cho Course Service.
 * Extends BaseWebSocketAuthInterceptor để xác thực JWT token.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor extends BaseWebSocketAuthInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    protected TokenValidationResult validateToken(String token) {
        try {
            // Validate token
            if (!jwtUtil.validateToken(token)) {
                log.warn("Invalid JWT token for WebSocket connection");
                return TokenValidationResult.invalid();
            }

            // Extract user info từ token
            UUID userId = jwtUtil.getUserIdFromToken(token);
            String email = jwtUtil.getEmailFromToken(token);
            List<String> roles = jwtUtil.getRolesFromToken(token);

            log.debug("WebSocket token validated for user: {} ({})", email, userId);
            return TokenValidationResult.valid(userId, email, roles);

        } catch (Exception e) {
            log.error("Error validating WebSocket JWT token: {}", e.getMessage());
            return TokenValidationResult.invalid();
        }
    }
}
