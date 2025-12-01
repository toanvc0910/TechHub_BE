package com.techhub.app.commonservice.websocket.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.StringUtils;

import java.security.Principal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Base interceptor để xác thực JWT token khi client CONNECT qua WebSocket.
 * Các service cần extend class này và implement phương thức validateToken().
 */
@Slf4j
public abstract class BaseWebSocketAuthInterceptor implements ChannelInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        // Chỉ xử lý CONNECT command
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractToken(accessor);

            if (!StringUtils.hasText(token)) {
                log.warn("WebSocket CONNECT without token");
                // Cho phép connect nhưng không có authentication
                // Có thể throw exception nếu muốn bắt buộc auth
                return message;
            }

            try {
                // Validate token và lấy thông tin user
                TokenValidationResult result = validateToken(token);

                if (result.isValid()) {
                    // Tạo authentication principal
                    List<SimpleGrantedAuthority> authorities = result.getRoles().stream()
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                            .collect(Collectors.toList());

                    Principal principal = new UsernamePasswordAuthenticationToken(
                            result.getUserId().toString(),
                            null,
                            authorities);

                    accessor.setUser(principal);
                    log.debug("WebSocket authenticated user: {}", result.getUserId());
                } else {
                    log.warn("WebSocket CONNECT with invalid token");
                }
            } catch (Exception e) {
                log.error("Error validating WebSocket token: {}", e.getMessage());
            }
        }

        return message;
    }

    /**
     * Extract JWT token từ STOMP headers.
     * Hỗ trợ:
     * - Header "Authorization: Bearer <token>"
     * - Header "token: <token>"
     */
    private String extractToken(StompHeaderAccessor accessor) {
        // Thử lấy từ Authorization header
        List<String> authHeaders = accessor.getNativeHeader(AUTHORIZATION_HEADER);
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String authHeader = authHeaders.get(0);
            if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
                return authHeader.substring(BEARER_PREFIX.length());
            }
        }

        // Thử lấy từ "token" header (fallback)
        List<String> tokenHeaders = accessor.getNativeHeader("token");
        if (tokenHeaders != null && !tokenHeaders.isEmpty()) {
            return tokenHeaders.get(0);
        }

        return null;
    }

    /**
     * Validate JWT token và trả về thông tin user.
     * Phương thức này cần được implement ở mỗi service.
     *
     * @param token JWT token
     * @return TokenValidationResult chứa thông tin validation
     */
    protected abstract TokenValidationResult validateToken(String token);

    /**
     * Result class cho việc validate token.
     */
    public static class TokenValidationResult {
        private final boolean valid;
        private final UUID userId;
        private final String email;
        private final List<String> roles;

        private TokenValidationResult(boolean valid, UUID userId, String email, List<String> roles) {
            this.valid = valid;
            this.userId = userId;
            this.email = email;
            this.roles = roles;
        }

        public static TokenValidationResult valid(UUID userId, String email, List<String> roles) {
            return new TokenValidationResult(true, userId, email, roles);
        }

        public static TokenValidationResult invalid() {
            return new TokenValidationResult(false, null, null, null);
        }

        public boolean isValid() {
            return valid;
        }

        public UUID getUserId() {
            return userId;
        }

        public String getEmail() {
            return email;
        }

        public List<String> getRoles() {
            return roles;
        }
    }
}
