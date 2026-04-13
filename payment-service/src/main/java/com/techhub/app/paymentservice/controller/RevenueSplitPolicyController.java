package com.techhub.app.paymentservice.controller;

import com.techhub.app.commonservice.payload.GlobalResponse;
import com.techhub.app.paymentservice.dto.request.UpsertRevenueSplitPolicyRequest;
import com.techhub.app.paymentservice.dto.response.RevenueSplitPolicyResponse;
import com.techhub.app.paymentservice.service.RevenueSplitPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/revenue-policies")
@RequiredArgsConstructor
public class RevenueSplitPolicyController {

    private final RevenueSplitPolicyService revenueSplitPolicyService;

    @PostMapping
    public ResponseEntity<GlobalResponse<RevenueSplitPolicyResponse>> createPolicy(
            @RequestHeader(value = "X-User-Roles", required = false) String userRoles,
            @RequestBody UpsertRevenueSplitPolicyRequest request) {
        if (!hasAdminRole(userRoles)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(GlobalResponse.error("Admin role is required", HttpStatus.FORBIDDEN.value()));
        }

        try {
            RevenueSplitPolicyResponse response = revenueSplitPolicyService.createPolicy(request);
            return ResponseEntity.ok(GlobalResponse.success("Revenue split policy created", response));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(GlobalResponse.error(ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
        }
    }

    @GetMapping
    public ResponseEntity<GlobalResponse<List<RevenueSplitPolicyResponse>>> listPolicies(
            @RequestHeader(value = "X-User-Roles", required = false) String userRoles,
            @RequestParam(required = false, defaultValue = "GLOBAL") String scope) {
        if (!hasAdminRole(userRoles)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(GlobalResponse.error("Admin role is required", HttpStatus.FORBIDDEN.value()));
        }

        try {
            List<RevenueSplitPolicyResponse> response = revenueSplitPolicyService.listByScope(scope);
            return ResponseEntity.ok(GlobalResponse.success("Revenue split policies", response));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(GlobalResponse.error(ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
        }
    }

    @GetMapping("/active")
    public ResponseEntity<GlobalResponse<RevenueSplitPolicyService.ResolvedPolicy>> getActivePolicy(
            @RequestHeader(value = "X-User-Roles", required = false) String userRoles,
            @RequestParam(required = false) UUID instructorId,
            @RequestParam(required = false) UUID courseId,
            @RequestParam(required = false) String refTime) {
        if (!hasAdminRole(userRoles)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(GlobalResponse.error("Admin role is required", HttpStatus.FORBIDDEN.value()));
        }

        OffsetDateTime parsedRefTime = null;
        if (refTime != null && !refTime.isBlank()) {
            try {
                parsedRefTime = OffsetDateTime.parse(refTime);
            } catch (Exception ex) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(GlobalResponse.error("Invalid refTime. Expected ISO OffsetDateTime",
                                HttpStatus.BAD_REQUEST.value()));
            }
        }

        RevenueSplitPolicyService.ResolvedPolicy response = revenueSplitPolicyService.resolvePolicy(instructorId,
                courseId, parsedRefTime);
        return ResponseEntity.ok(GlobalResponse.success("Active revenue split policy", response));
    }

    private boolean hasAdminRole(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) {
            return false;
        }
        return rolesHeader.toUpperCase().contains("ADMIN");
    }
}