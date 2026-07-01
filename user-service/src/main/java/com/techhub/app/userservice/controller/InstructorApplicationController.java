package com.techhub.app.userservice.controller;

import com.techhub.app.commonservice.payload.GlobalResponse;
import com.techhub.app.userservice.dto.request.CreateInstructorApplicationRequest;
import com.techhub.app.userservice.dto.request.ReviewInstructorApplicationRequest;
import com.techhub.app.userservice.dto.response.InstructorApplicationResponse;
import com.techhub.app.userservice.enums.InstructorApplicationAdminStatus;
import com.techhub.app.userservice.service.InstructorApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/instructor-applications")
@RequiredArgsConstructor
@Slf4j
public class InstructorApplicationController {

    private final InstructorApplicationService service;

    @PostMapping
    public ResponseEntity<GlobalResponse<InstructorApplicationResponse>> submit(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Valid @RequestBody CreateInstructorApplicationRequest request) {
        try {
            UUID uid = parseUid(userId);
            InstructorApplicationResponse resp = service.submit(uid, request);
            return ResponseEntity.ok(GlobalResponse.success("Đã gửi đơn ứng tuyển", resp));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(GlobalResponse.error(ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<GlobalResponse<List<InstructorApplicationResponse>>> getMine(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            UUID uid = parseUid(userId);
            return ResponseEntity.ok(GlobalResponse.success("My applications", service.getMyApplications(uid)));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(GlobalResponse.error(ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
        }
    }

    @GetMapping
    public ResponseEntity<GlobalResponse<Map<String, Object>>> listForAdmin(
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        if (!hasAdminRole(roles)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(GlobalResponse.error("Admin role required", HttpStatus.FORBIDDEN.value()));
        }
        InstructorApplicationAdminStatus filter = null;
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            try {
                filter = InstructorApplicationAdminStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }
        Page<InstructorApplicationResponse> result = service.listForAdmin(filter, page, size);
        Map<String, Object> body = new HashMap<>();
        body.put("content", result.getContent());
        body.put("totalElements", result.getTotalElements());
        body.put("totalPages", result.getTotalPages());
        body.put("number", result.getNumber());
        body.put("size", result.getSize());
        return ResponseEntity.ok(GlobalResponse.success("Applications", body));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GlobalResponse<InstructorApplicationResponse>> getDetail(
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable UUID id) {
        try {
            InstructorApplicationResponse resp = service.getDetail(id);
            // Owner hoặc admin mới được xem.
            if (!hasAdminRole(roles)) {
                UUID uid = parseUid(userId);
                if (!uid.equals(resp.getUserId())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(GlobalResponse.error("Forbidden", HttpStatus.FORBIDDEN.value()));
                }
            }
            return ResponseEntity.ok(GlobalResponse.success("Application detail", resp));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(GlobalResponse.error(ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
        }
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<GlobalResponse<InstructorApplicationResponse>> approve(
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable UUID id,
            @RequestBody(required = false) ReviewInstructorApplicationRequest request) {
        if (!hasAdminRole(roles)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(GlobalResponse.error("Admin role required", HttpStatus.FORBIDDEN.value()));
        }
        try {
            UUID reviewerId = parseUid(userId);
            return ResponseEntity.ok(GlobalResponse.success("Approved",
                    service.approve(id, reviewerId, request)));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(GlobalResponse.error(ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
        }
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<GlobalResponse<InstructorApplicationResponse>> reject(
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable UUID id,
            @RequestBody(required = false) ReviewInstructorApplicationRequest request) {
        if (!hasAdminRole(roles)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(GlobalResponse.error("Admin role required", HttpStatus.FORBIDDEN.value()));
        }
        try {
            UUID reviewerId = parseUid(userId);
            return ResponseEntity.ok(GlobalResponse.success("Rejected",
                    service.reject(id, reviewerId, request)));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(GlobalResponse.error(ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
        }
    }

    @PostMapping("/{id}/rescan/cv")
    public ResponseEntity<GlobalResponse<Void>> rescanCv(
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @PathVariable UUID id) {
        if (!hasAdminRole(roles)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(GlobalResponse.error("Admin role required", HttpStatus.FORBIDDEN.value()));
        }
        try {
            service.rescanCv(id);
            return ResponseEntity.ok(GlobalResponse.success("Đã gửi lại quét CV", null));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(GlobalResponse.error(ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
        }
    }

    @PostMapping("/{id}/rescan/cccd-front")
    public ResponseEntity<GlobalResponse<Void>> rescanCccdFront(
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @PathVariable UUID id) {
        if (!hasAdminRole(roles)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(GlobalResponse.error("Admin role required", HttpStatus.FORBIDDEN.value()));
        }
        try {
            service.rescanCccd(id, true);
            return ResponseEntity.ok(GlobalResponse.success("Đã gửi lại quét CCCD mặt trước", null));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(GlobalResponse.error(ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
        }
    }

    @PostMapping("/{id}/rescan/cccd-back")
    public ResponseEntity<GlobalResponse<Void>> rescanCccdBack(
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @PathVariable UUID id) {
        if (!hasAdminRole(roles)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(GlobalResponse.error("Admin role required", HttpStatus.FORBIDDEN.value()));
        }
        try {
            service.rescanCccd(id, false);
            return ResponseEntity.ok(GlobalResponse.success("Đã gửi lại quét CCCD mặt sau", null));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(GlobalResponse.error(ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
        }
    }

    @PostMapping("/certificates/{certId}/rescan")
    public ResponseEntity<GlobalResponse<Void>> rescanCertificate(
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @PathVariable UUID certId) {
        if (!hasAdminRole(roles)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(GlobalResponse.error("Admin role required", HttpStatus.FORBIDDEN.value()));
        }
        try {
            service.rescanCertificate(certId);
            return ResponseEntity.ok(GlobalResponse.success("Đã gửi lại quét chứng chỉ", null));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(GlobalResponse.error(ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
        }
    }

    private UUID parseUid(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("Missing X-User-Id header");
        }
        try {
            return UUID.fromString(userId);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid X-User-Id");
        }
    }

    private boolean hasAdminRole(String roles) {
        return roles != null && roles.toUpperCase().contains("ADMIN");
    }
}
