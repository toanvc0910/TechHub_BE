package com.techhub.app.proxyclient.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techhub.app.commonservice.dto.EndpointSecurityPolicyDTO;
import com.techhub.app.commonservice.enums.SecurityLevel;
import com.techhub.app.proxyclient.client.UserServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory cache of endpoint security policies loaded from user-service.
 * Reloaded on startup and on Kafka events.
 *
 * Matching logic:
 * 1. Iterate all active policies
 * 2. Match incoming (url, method) against each policy's ant-style urlPattern +
 * method
 * 3. First match wins (most-specific patterns should be inserted first in DB)
 * 4. If no match → default is AUTHORIZED (JWT + permission check)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EndpointSecurityCacheService {

    private final UserServiceClient userServiceClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private static final long RELOAD_RETRY_INTERVAL_MS = 10_000;

    private final List<EndpointSecurityPolicyDTO> policies = new CopyOnWriteArrayList<>();
    private volatile long lastReloadAttemptMs = 0;

    private static final List<String> BOOTSTRAP_PUBLIC_PATTERNS = Arrays.asList(
            "/api/auth/**",
            "/api/users",
            "/api/users/forgot-password",
            "/api/users/reset-password/**",
            "/api/users/resend-reset-code/**",
            "/api/users/public/**",
            "/api/internal/endpoint-security-policies",
            "/actuator/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/oauth2/**");

    @EventListener(ApplicationReadyEvent.class)
    public void loadOnStartup() {
        reload();
    }

    public void reload() {
        lastReloadAttemptMs = System.currentTimeMillis();
        try {
            ResponseEntity<String> response = userServiceClient.getEndpointSecurityPolicies();
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<?, ?> body = objectMapper.readValue(response.getBody(), Map.class);
                Object data = body.get("data");
                List<EndpointSecurityPolicyDTO> loaded = objectMapper.convertValue(data,
                        new TypeReference<List<EndpointSecurityPolicyDTO>>() {
                        });
                policies.clear();
                policies.addAll(loaded);
                log.info("Loaded {} endpoint security policies from user-service", policies.size());
                policies.forEach(p -> log.debug("  {} {} -> {}", p.getMethod(), p.getUrlPattern(),
                        p.getSecurityLevel()));
            } else {
                log.warn("Failed to load endpoint security policies: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error loading endpoint security policies — will use default AUTHORIZED for all endpoints", e);
        }
    }

    /**
     * Resolve the security level for an incoming request.
     *
     * @param url    the normalized URL path (e.g. /api/courses/123)
     * @param method the HTTP method (GET, POST, etc.)
     * @return the matching SecurityLevel, or AUTHORIZED if no policy matches
     */
    public SecurityLevel resolve(String url, String method) {
        if (policies.isEmpty()) {
            tryReloadWhenEmpty();
        }

        for (EndpointSecurityPolicyDTO policy : policies) {
            boolean methodMatch = "*".equals(policy.getMethod())
                    || policy.getMethod().equalsIgnoreCase(method);
            if (methodMatch && pathMatcher.match(policy.getUrlPattern(), url)) {
                return policy.getSecurityLevel();
            }
        }

        if (isBootstrapPublic(url, method)) {
            return SecurityLevel.PUBLIC;
        }

        return SecurityLevel.AUTHORIZED;
    }

    private void tryReloadWhenEmpty() {
        long now = System.currentTimeMillis();
        if ((now - lastReloadAttemptMs) < RELOAD_RETRY_INTERVAL_MS) {
            return;
        }
        reload();
    }

    private boolean isBootstrapPublic(String url, String method) {
        if (!"GET".equalsIgnoreCase(method)
                && !"POST".equalsIgnoreCase(method)
                && !"PUT".equalsIgnoreCase(method)
                && !"DELETE".equalsIgnoreCase(method)
                && !"PATCH".equalsIgnoreCase(method)
                && !"OPTIONS".equalsIgnoreCase(method)
                && !"HEAD".equalsIgnoreCase(method)) {
            return false;
        }

        return BOOTSTRAP_PUBLIC_PATTERNS.stream().anyMatch(pattern -> pathMatcher.match(pattern, url));
    }
}
