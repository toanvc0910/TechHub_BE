package com.techhub.app.proxyclient.security;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Check if user has permission to access URL with method
     * Real-time permission check without caching for security
     * Flow:
     * 1. Call User Service directly via REST
     * 2. Return result immediately
     */
    public boolean hasPermission(UUID userId, String url, String method, String authHeader) {
        log.info("🔍 [PermissionGatewayService] ========== PERMISSION CHECK START ==========");
        log.info("🔍 [PermissionGatewayService] UserId: {}, Method: {}, URL: {}", userId, method, url);

        try {
            // Call User Service directly - no cache for real-time permission enforcement
            log.info("🌐 [PermissionGatewayService] Calling User Service REST API...");
            PermissionCheckRequest req = new PermissionCheckRequest(url, method);
            log.info("🌐 [PermissionGatewayService] Request: POST /api/users/{}/permissions/check", userId);

            ResponseEntity<String> response = userServiceClient.checkPermission(userId.toString(), req, authHeader);

            log.info("📥 [PermissionGatewayService] Response: {} - {}", response.getStatusCode(), response.getBody());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<?, ?> body = objectMapper.readValue(response.getBody(), Map.class);
                Object data = body.get("data");

                if (data instanceof Boolean) {
                    boolean allowed = (Boolean) data;
                    log.info("✅ [PermissionGatewayService] Result: {} ({})",
                            allowed, allowed ? "ALLOWED ✅" : "DENIED ❌");
                    log.info("🔍 [PermissionGatewayService] ========== PERMISSION CHECK END ==========");
                    return allowed;
                } else {
                    log.error("❌ [PermissionGatewayService] Invalid data type: expected Boolean, got {}",
                            data != null ? data.getClass().getName() : "null");
                }
            } else {
                log.error("❌ [PermissionGatewayService] Invalid response - Status: {}, Body: {}",
                        response.getStatusCode(), response.getBody());
            }

            log.warn("⚠️ [PermissionGatewayService] Unexpected response, denying access");
            log.info("🔍 [PermissionGatewayService] ========== PERMISSION CHECK END ==========");
            return false;
        } catch (Exception e) {
            log.error("❌ [PermissionGatewayService] Exception during permission check: {}", e.getMessage(), e);
            log.info("🔍 [PermissionGatewayService] ========== PERMISSION CHECK END ==========");
            return false;
        }
    }
}
