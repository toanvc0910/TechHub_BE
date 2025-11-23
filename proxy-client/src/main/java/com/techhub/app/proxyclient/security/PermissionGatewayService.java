package com.techhub.app.proxyclient.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techhub.app.proxyclient.cache.PermissionCacheService;
import com.techhub.app.proxyclient.client.UserServiceClient;
import com.techhub.app.proxyclient.dto.PermissionCheckRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionGatewayService {

    private final UserServiceClient userServiceClient;
    private final PermissionCacheService permissionCacheService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Check if user has permission to access URL with method
     * Flow:
     * 1. Check Redis cache first
     * 2. If cache miss, call User Service via REST
     * 3. Cache the result for 5 minutes
     * 4. Return result
     */
    public boolean hasPermission(UUID userId, String url, String method, String authHeader) {
        log.info("🔍 [PermissionGatewayService] Checking permission for user {} on {} {}", userId, method, url);

        try {
            // Step 1: Check cache first
            log.info("📦 [PermissionGatewayService] Checking Redis cache...");
            Boolean cachedResult = permissionCacheService.getPermission(userId, url, method);
            if (cachedResult != null) {
                log.info("✅ [PermissionGatewayService] Cache HIT - Permission: {}", cachedResult);
                return cachedResult;
            }
            log.info("❌ [PermissionGatewayService] Cache MISS - Calling User Service...");

            // Step 2: Cache miss - call User Service
            PermissionCheckRequest req = new PermissionCheckRequest(url, method);
            log.info("🌐 [PermissionGatewayService] Calling User Service REST API for permission check...");
            ResponseEntity<String> response = userServiceClient.checkPermission(userId.toString(), req, authHeader);
            log.info("📥 [PermissionGatewayService] User Service response status: {}", response.getStatusCode());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("📄 [PermissionGatewayService] Response body: {}", response.getBody());
                Map<?, ?> body = objectMapper.readValue(response.getBody(), Map.class);
                Object data = body.get("data");

                if (data instanceof Boolean) {
                    boolean allowed = (Boolean) data;
                    log.info("✅ [PermissionGatewayService] Permission result: {}", allowed);

                    // Step 3: Cache the result
                    log.info("💾 [PermissionGatewayService] Caching permission result...");
                    permissionCacheService.cachePermission(userId, url, method, allowed);

                    return allowed;
                }
            }

            log.warn("⚠️ [PermissionGatewayService] Permission check unexpected response for user {} on {} {}: {}",
                    userId, method, url, response);
            return false;
        } catch (Exception e) {
            log.error("❌ [PermissionGatewayService] Permission check failed for user {} on {} {} - Error: {}", userId,
                    method, url, e.getMessage(), e);
            return false;
        }
    }
}
