package com.techhub.app.proxyclient.security;

import com.techhub.app.commonservice.enums.SecurityLevel;
import com.techhub.app.commonservice.enums.UserRole;
import com.techhub.app.commonservice.jwt.JwtUtil;
import com.techhub.app.proxyclient.cache.EndpointSecurityCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JWT Authentication Filter for Proxy-Client.
 * This is the ONLY place where JWT is validated in the system.
 * Security level per endpoint is driven by DB (endpoint_security_policies
 * table):
 * <ul>
 * <li>PUBLIC — skip JWT, skip permission</li>
 * <li>AUTHENTICATED — validate JWT, skip permission</li>
 * <li>AUTHORIZED — validate JWT + RBAC permission check (default when no policy
 * matches)</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final PermissionGatewayService permissionGatewayService;
    private final EndpointSecurityCacheService endpointSecurityCacheService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        String normalizedPath = normalizeTargetPath(requestURI);

        // Resolve security level from cached DB policies
        SecurityLevel level = endpointSecurityCacheService.resolve(normalizedPath, method);

        // PUBLIC → no JWT, no permission
        if (level == SecurityLevel.PUBLIC) {
            filterChain.doFilter(request, response);
            return;
        }

        // AUTHENTICATED or AUTHORIZED → JWT required
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter()
                    .write("{\"error\":\"Unauthorized\",\"message\":\"JWT token is required\"}");
            return;
        }

        String jwt = authHeader.substring(7);

        try {
            if (!jwtUtil.validateToken(jwt)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter()
                        .write("{\"error\":\"Unauthorized\",\"message\":\"Invalid or expired JWT token\"}");
                return;
            }

            UUID userId = jwtUtil.getUserIdFromToken(jwt);
            String email = jwtUtil.getEmailFromToken(jwt);
            List<String> roles = jwtUtil.getRolesFromToken(jwt);

            // Set authentication context
            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userId, null,
                    authorities);
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);

            // Add user info to request for Feign clients to forward
            request.setAttribute("userId", userId);
            request.setAttribute("userEmail", email);
            request.setAttribute("userRoles", roles);
            request.setAttribute("jwt", jwt);

            // AUTHENTICATED → JWT valid, skip permission check
            if (level == SecurityLevel.AUTHENTICATED) {
                filterChain.doFilter(request, response);
                return;
            }

            // AUTHORIZED → JWT valid + RBAC permission check (ADMIN bypasses)
            if (hasBypassRole(roles)) {
                filterChain.doFilter(request, response);
                return;
            }

            boolean allowed = permissionGatewayService.hasPermission(userId, normalizedPath, method, authHeader);
            if (!allowed) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"error\":\"Access denied\",\"message\":\"You don't have permission to access this resource\"}");
                return;
            }

        } catch (Exception e) {
            log.error("JWT validation exception for: {} {} - {}", method, requestURI, e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter()
                    .write("{\"error\":\"Unauthorized\",\"message\":\"JWT token validation failed\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String normalizeTargetPath(String uri) {
        if (uri.startsWith("/app/api/proxy")) {
            return uri.replaceFirst("/app/api/proxy", "/api");
        }
        if (uri.startsWith("/api/proxy")) {
            return uri.replaceFirst("/api/proxy", "/api");
        }
        return uri;
    }

    private boolean hasBypassRole(List<String> roles) {
        return roles != null && roles.stream().anyMatch(r -> UserRole.ADMIN.name().equalsIgnoreCase(r));
    }
}
