package com.techhub.app.commonservice.context;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;

/**
 * Utility class to access current user context from any service
 * User context is set by UserContextInterceptor from headers sent by proxy-client
 */
@Slf4j
public class UserContext {

    /**
     * Get current authenticated user ID
     */
    public static UUID getCurrentUserId() {
        try {
            HttpServletRequest request = getCurrentRequest();
            if (request != null) {
                Object userId = request.getAttribute("currentUserId");
                return userId != null ? (UUID) userId : null;
            }
        } catch (Exception e) {
            log.warn("Failed to get current user ID", e);
        }
        return null;
    }

    /**
     * Get current authenticated user email
     */
    public static String getCurrentUserEmail() {
        try {
            HttpServletRequest request = getCurrentRequest();
            if (request != null) {
                Object userEmail = request.getAttribute("currentUserEmail");
                return userEmail != null ? (String) userEmail : null;
            }
        } catch (Exception e) {
            log.warn("Failed to get current user email", e);
        }
        return null;
    }

    /**
     * Get current authenticated user roles
     */
    @SuppressWarnings("unchecked")
    public static List<String> getCurrentUserRoles() {
        try {
            HttpServletRequest request = getCurrentRequest();
            if (request != null) {
                Object userRoles = request.getAttribute("currentUserRoles");
                return userRoles != null ? (List<String>) userRoles : null;
            }
        } catch (Exception e) {
            log.warn("Failed to get current user roles", e);
        }
        return null;
    }

    /**
     * Check if current user has specific role
     */
    public static boolean hasRole(String role) {
        List<String> roles = getCurrentUserRoles();
        return roles != null && roles.contains(role);
    }

    /**
     * Check if current user has any of the specified roles
     */
    public static boolean hasAnyRole(String... roles) {
        List<String> userRoles = getCurrentUserRoles();
        if (userRoles == null) return false;

        for (String role : roles) {
            if (userRoles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get current HTTP request
     */
    private static HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    /**
     * Check if user is authenticated (has user context)
     */
    public static boolean isAuthenticated() {
        return getCurrentUserId() != null;
    }
}
