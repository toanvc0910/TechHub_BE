package com.techhub.app.userservice.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techhub.app.commonservice.jwt.JwtUtil;
import com.techhub.app.commonservice.payload.GlobalResponse;
import com.techhub.app.userservice.entity.Permission;
import com.techhub.app.userservice.entity.Role;
import com.techhub.app.userservice.entity.User;
import com.techhub.app.userservice.enums.PermissionMethod;
import com.techhub.app.userservice.repository.PermissionRepository;
import com.techhub.app.userservice.repository.RolePermissionRepository;
import com.techhub.app.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class PermissionInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        String httpMethod = request.getMethod();

        log.debug("Checking permission for: {} {}", httpMethod, requestURI);

        // Skip cho các endpoint công khai
        if (isPublicEndpoint(requestURI, httpMethod)) {
            return true;
        }

        // Lấy token từ header
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return handleUnauthorized(response, "Missing or invalid authorization header", requestURI);
        }

        String token = authHeader.substring(7);

        try {
            // Validate token
            if (!jwtUtil.validateToken(token)) {
                return handleUnauthorized(response, "Invalid or expired token", requestURI);
            }

            // Lấy user từ token
            UUID userId = jwtUtil.getUserIdFromToken(token);
            User user = userRepository.findById(userId).orElse(null);

            if (user == null || !user.getIsActive()) {
                return handleUnauthorized(response, "User not found or inactive", requestURI);
            }

            // Kiểm tra quyền chi tiết
            if (!hasPermission(user, requestURI, httpMethod)) {
                return handleForbidden(response, "Access denied - insufficient permissions", requestURI);
            }

            // Đặt user info vào request attribute để controller sử dụng
            request.setAttribute("currentUser", user);
            request.setAttribute("currentUserId", userId);

            return true;

        } catch (Exception e) {
            log.error("Error in permission check", e);
            return handleUnauthorized(response, "Token validation failed", requestURI);
        }
    }

    private boolean isPublicEndpoint(String uri, String method) {
        // Danh sách các endpoint công khai
        return (uri.startsWith("/api/auth/") && !uri.equals("/api/auth/validate")) ||
                (uri.equals("/api/users") && "POST".equals(method)) || // Register
                uri.startsWith("/api/users/forgot-password") ||
                uri.startsWith("/api/users/reset-password") ||
                uri.startsWith("/actuator/") ||
                uri.startsWith("/swagger-ui/") ||
                uri.startsWith("/v3/api-docs/");
    }

    private boolean hasPermission(User user, String requestURI, String httpMethod) {
        // Lấy tất cả roles của user
        List<Role> userRoles = user.getUserRoles().stream()
                .filter(ur -> ur.getIsActive())
                .map(ur -> ur.getRole())
                .filter(role -> role != null && role.getIsActive())
                .collect(Collectors.toList());

        if (userRoles.isEmpty()) {
            log.warn("User {} has no active roles", user.getId());
            return false;
        }

        // Chuyển đổi HTTP method
        PermissionMethod permissionMethod = getPermissionMethod(httpMethod);
        if (permissionMethod == null) {
            log.warn("Unsupported HTTP method: {}", httpMethod);
            return false;
        }

        // Tìm permission phù hợp
        List<Permission> requiredPermissions = findMatchingPermissions(requestURI, permissionMethod);

        if (requiredPermissions.isEmpty()) {
            log.debug("No specific permission required for: {} {}", httpMethod, requestURI);
            return true; // Nếu không có permission cụ thể, cho phép
        }

        // Kiểm tra user có ít nhất 1 permission phù hợp không thông qua RolePermission
        for (Role role : userRoles) {
            // Lấy permissions của role thông qua RolePermission repository
            List<Permission> rolePermissions = getRolePermissions(role.getId());

            for (Permission requiredPerm : requiredPermissions) {
                if (rolePermissions.stream().anyMatch(p -> p.getId().equals(requiredPerm.getId()))) {
                    log.debug("User {} has permission {} through role {}",
                            user.getId(), requiredPerm.getName(), role.getName());
                    return true;
                }
            }
        }

        log.warn("User {} does not have required permissions for: {} {}",
                user.getId(), httpMethod, requestURI);
        return false;
    }

    private List<Permission> getRolePermissions(UUID roleId) {
        // Truy vấn permissions của role thông qua RolePermission
        return permissionRepository.findAll().stream()
                .filter(permission -> {
                    // Check xem permission này có trong role không thông qua role_permissions table
                    return rolePermissionRepository.findByRoleIdAndPermissionIdAndIsActive(roleId, permission.getId(), true).isPresent();
                })
                .collect(Collectors.toList());
    }

    private List<Permission> findMatchingPermissions(String requestURI, PermissionMethod method) {
        // Tìm permissions khớp với URI và method
        return permissionRepository.findByMethodAndIsActive(method, true).stream()
                .filter(permission -> uriMatches(requestURI, permission.getUrl()))
                .collect(Collectors.toList());
    }

    private boolean uriMatches(String requestURI, String permissionURL) {
        // Simple pattern matching - có thể mở rộng để support regex patterns
        if (permissionURL.equals(requestURI)) {
            return true;
        }

        // Handle path variables như /api/users/{id}
        String[] requestParts = requestURI.split("/");
        String[] permissionParts = permissionURL.split("/");

        if (requestParts.length != permissionParts.length) {
            return false;
        }

        for (int i = 0; i < requestParts.length; i++) {
            if (!permissionParts[i].startsWith("{") && !permissionParts[i].equals(requestParts[i])) {
                return false;
            }
        }

        return true;
    }

    private PermissionMethod getPermissionMethod(String httpMethod) {
        try {
            return PermissionMethod.valueOf(httpMethod);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private boolean handleUnauthorized(HttpServletResponse response, String message, String path) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        GlobalResponse<Object> errorResponse = GlobalResponse.error(message, 401).withPath(path);

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        return false;
    }

    private boolean handleForbidden(HttpServletResponse response, String message, String path) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        GlobalResponse<Object> errorResponse = GlobalResponse.error(message, 403).withPath(path);

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        return false;
    }
}
