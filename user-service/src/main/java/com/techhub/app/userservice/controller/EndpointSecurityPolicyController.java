package com.techhub.app.userservice.controller;

import com.techhub.app.commonservice.dto.EndpointSecurityPolicyDTO;
import com.techhub.app.commonservice.enums.SecurityLevel;
import com.techhub.app.commonservice.payload.GlobalResponse;
import com.techhub.app.userservice.service.EndpointSecurityPolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class EndpointSecurityPolicyController {

        private final EndpointSecurityPolicyService service;

        /**
         * Internal endpoint — called by proxy-client at startup / cache reload.
         * No auth required (user-service SecurityConfig is permitAll for internal
         * calls).
         */
        @GetMapping("/api/internal/endpoint-security-policies")
        public ResponseEntity<GlobalResponse<List<EndpointSecurityPolicyDTO>>> listPolicies(
                        HttpServletRequest request) {
                List<EndpointSecurityPolicyDTO> policies = service.listActivePolicies();
                return ResponseEntity.ok(
                                GlobalResponse.success("Endpoint security policies fetched", policies)
                                                .withPath(request.getRequestURI()));
        }

        // ===== Admin CRUD =====

        @PostMapping("/api/admin/endpoint-security-policies")
        public ResponseEntity<GlobalResponse<EndpointSecurityPolicyDTO>> createPolicy(
                        @RequestBody PolicyRequest body,
                        @RequestHeader(value = "X-User-Id", required = false) String actorHeader,
                        HttpServletRequest request) {
                UUID actor = parseUuid(actorHeader);
                EndpointSecurityPolicyDTO result = service.createPolicy(
                                body.urlPattern, body.method, body.securityLevel, body.description, actor);
                return ResponseEntity.ok(
                                GlobalResponse.success("Policy created", result)
                                                .withPath(request.getRequestURI()));
        }

        @PutMapping("/api/admin/endpoint-security-policies/{id}")
        public ResponseEntity<GlobalResponse<EndpointSecurityPolicyDTO>> updatePolicy(
                        @PathVariable UUID id,
                        @RequestBody PolicyRequest body,
                        @RequestHeader(value = "X-User-Id", required = false) String actorHeader,
                        HttpServletRequest request) {
                UUID actor = parseUuid(actorHeader);
                EndpointSecurityPolicyDTO result = service.updatePolicy(
                                id, body.urlPattern, body.method, body.securityLevel, body.description, actor);
                return ResponseEntity.ok(
                                GlobalResponse.success("Policy updated", result)
                                                .withPath(request.getRequestURI()));
        }

        @DeleteMapping("/api/admin/endpoint-security-policies/{id}")
        public ResponseEntity<GlobalResponse<?>> deletePolicy(
                        @PathVariable UUID id,
                        @RequestHeader(value = "X-User-Id", required = false) String actorHeader,
                        HttpServletRequest request) {
                UUID actor = parseUuid(actorHeader);
                service.deletePolicy(id, actor);
                return ResponseEntity.ok(
                                GlobalResponse.success("Policy deleted", null)
                                                .withPath(request.getRequestURI()));
        }

        private UUID parseUuid(String raw) {
                try {
                        return raw != null && !raw.isBlank() ? UUID.fromString(raw) : null;
                } catch (IllegalArgumentException e) {
                        return null;
                }
        }

        /**
         * Request body for policy CRUD.
         */
        static class PolicyRequest {
                public String urlPattern;
                public String method;
                public SecurityLevel securityLevel;
                public String description;
        }
}
