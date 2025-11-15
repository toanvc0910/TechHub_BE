package com.techhub.app.proxyclient.security;

import com.techhub.app.commonservice.jwt.JwtUtil;
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
 * JWT Authentication Filter for Proxy-Client
 * This is the ONLY place where JWT is validated in the system
 * Other microservices receive user info via headers
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        // Skip JWT validation for public endpoints
        if (isPublicEndpoint(requestURI, method)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7);

        try {
            // Validate JWT token - ONLY validation point in the system
            if (jwtUtil.validateToken(jwt)) {
                UUID userId = jwtUtil.getUserIdFromToken(jwt);
                String email = jwtUtil.getEmailFromToken(jwt);
                List<String> roles = jwtUtil.getRolesFromToken(jwt);

                // Set authentication context
                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                // Add user info to request for Feign clients to forward
                request.setAttribute("userId", userId);
                request.setAttribute("userEmail", email);
                request.setAttribute("userRoles", roles);
                request.setAttribute("jwt", jwt);

                log.debug("JWT authenticated user: {} for: {} {}", userId, method, requestURI);
            }
        } catch (Exception e) {
            log.error("JWT validation failed for: {} {} - Error: {}", method, requestURI, e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(String uri, String method) {
        return uri.startsWith("/api/auth/") ||
               uri.startsWith("/api/proxy/auth/") ||
               (uri.equals("/api/users") && "POST".equals(method)) ||
               (uri.equals("/api/proxy/users") && "POST".equals(method)) ||
               uri.startsWith("/api/users/forgot-password") ||
               uri.startsWith("/api/proxy/users/forgot-password") ||
               uri.startsWith("/api/users/reset-password") ||
               uri.startsWith("/api/proxy/users/reset-password") ||
               uri.startsWith("/actuator/") ||
               uri.startsWith("/swagger-ui/") ||
               uri.startsWith("/v3/api-docs/") ||
               uri.startsWith("/oauth2/") ||
               uri.startsWith("/api/proxy/files/") ||
               uri.startsWith("/api/proxy/folders/") ||
               uri.startsWith("/api/proxy/file-usage/") ||
               uri.startsWith("/api/proxy/payments/");
    }
}
