package com.techhub.app.commonservice.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Interceptor to extract user context from headers sent by proxy-client
 * All microservices (except proxy-client) should use this interceptor
 */
@Component
@Slf4j
public class UserContextInterceptor implements HandlerInterceptor {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_EMAIL_HEADER = "X-User-Email";
    public static final String USER_ROLES_HEADER = "X-User-Roles";
    public static final String REQUEST_SOURCE_HEADER = "X-Request-Source";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        // Skip for health check and public endpoints
        if (isPublicEndpoint(requestURI)) {
            return true;
        }

        // Extract user context from headers
        String userIdStr = request.getHeader(USER_ID_HEADER);
        String userEmail = request.getHeader(USER_EMAIL_HEADER);
        String userRoles = request.getHeader(USER_ROLES_HEADER);
        String requestSource = request.getHeader(REQUEST_SOURCE_HEADER);

        // Validate that request comes from proxy-client or other internal services
        if (!"proxy-client".equals(requestSource) && !isInternalService(requestSource)) {
            log.warn("Request not from proxy-client or internal service - URI: {} {}, Source: {}", method, requestURI,
                    requestSource);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }

        // Parse and set user context
        if (userIdStr != null && userEmail != null) {
            try {
                UUID userId = UUID.fromString(userIdStr);
                List<String> roles = userRoles != null ? Arrays.asList(userRoles.split(",")) : Arrays.asList();

                // Set user context in request attributes
                request.setAttribute("currentUserId", userId);
                request.setAttribute("currentUserEmail", userEmail);
                request.setAttribute("currentUserRoles", roles);

                log.debug("User context set - ID: {}, Email: {}, Roles: {} for {} {}",
                        userId, userEmail, roles, method, requestURI);

                return true;
            } catch (Exception e) {
                log.error("Failed to parse user context from headers", e);
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return false;
            }
        }

        // No user context found - this should not happen for protected endpoints
        log.warn("No user context found in headers for protected endpoint: {} {}", method, requestURI);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return false;
    }

    private boolean isPublicEndpoint(String uri) {
        return uri.startsWith("/actuator/") ||
                uri.startsWith("/swagger-ui/") ||
                uri.startsWith("/v3/api-docs/") ||
                uri.equals("/health") ||
                uri.equals("/api/health") ||
                // Auth endpoints should be public (handled by proxy-client)
                uri.startsWith("/api/auth/") ||
                // User registration endpoint
                (uri.equals("/api/users") && "POST".equals("POST")) ||
                // Password reset endpoints
                uri.startsWith("/api/users/forgot-password") ||
                uri.startsWith("/api/users/reset-password/") ||
                uri.startsWith("/api/users/resend-reset-code/") ||
                // Public user endpoints
                uri.startsWith("/api/users/public/") ||
                // Internal service-to-service endpoints (no auth required)
                uri.equals("/api/users/internal/all-user-ids") ||
                // OAuth2 endpoints
                uri.startsWith("/oauth2/") ||
                // AI Chat streaming endpoints (SSE - bypass proxy for real-time streaming)
                uri.startsWith("/api/ai/chat/stream");
    }

    private boolean isInternalService(String requestSource) {
        // Allow requests from internal microservices
        return requestSource != null && (requestSource.equals("payment-service") ||
                requestSource.equals("course-service") ||
                requestSource.equals("user-service") ||
                requestSource.equals("notification-service") ||
                requestSource.equals("learning-path-service") ||
                requestSource.equals("blog-service") ||
                requestSource.equals("file-service"));
    }
}
