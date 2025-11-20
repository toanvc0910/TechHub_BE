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

    public boolean hasPermission(UUID userId, String url, String method, String authHeader) {
        try {
            PermissionCheckRequest req = new PermissionCheckRequest(url, method);
            ResponseEntity<String> response = userServiceClient.checkPermission(userId.toString(), req, authHeader);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<?, ?> body = objectMapper.readValue(response.getBody(), Map.class);
                Object data = body.get("data");
                if (data instanceof Boolean) {
                    return (Boolean) data;
                }
            }
            log.warn("Permission check unexpected response for user {} on {} {}: {}", userId, method, url, response);
            return false;
        } catch (Exception e) {
            log.error("Permission check failed for user {} on {} {}", userId, method, url, e);
            return false;
        }
    }
}
