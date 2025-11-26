package com.techhub.app.courseservice.controller;

import com.techhub.app.commonservice.jwt.JwtUtil;
import com.techhub.app.courseservice.dto.request.CreateEnrollmentRequest;
import com.techhub.app.courseservice.dto.response.EnrollmentResponse;
import com.techhub.app.courseservice.enums.EnrollmentStatus;
import com.techhub.app.courseservice.service.EnrollmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/enrollments")
@Slf4j
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final JwtUtil jwtUtil;

    public EnrollmentController(EnrollmentService enrollmentService, JwtUtil jwtUtil) {
        this.enrollmentService = enrollmentService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createEnrollment(@Valid @RequestBody CreateEnrollmentRequest request) {
        log.info("💳 Received request to create enrollment for user: {} and course: {}",
                request.getUserId(), request.getCourseId());

        try {
            EnrollmentResponse enrollment = enrollmentService.createEnrollment(request);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Enrollment created successfully");
            response.put("data", enrollment);

            log.info("✅ Enrollment created successfully: {}", enrollment.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            // Duplicate enrollment or validation error
            log.warn("⚠️ Duplicate or invalid enrollment request: {}", e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
        } catch (RuntimeException e) {
            log.error("❌ Error creating enrollment: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to create enrollment: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/{enrollmentId}")
    public ResponseEntity<Map<String, Object>> getEnrollment(@PathVariable UUID enrollmentId) {
        log.info("Received request to get enrollment with ID: {}", enrollmentId);

        try {
            EnrollmentResponse enrollment = enrollmentService.getEnrollment(enrollmentId);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("data", enrollment);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting enrollment: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to get enrollment: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    /**
     * Get current user's enrollments
     * Lấy danh sách khóa học mà user hiện tại đã enroll
     */
    @GetMapping("/my-enrollments")
    public ResponseEntity<Map<String, Object>> getMyEnrollments(
            HttpServletRequest request,
            @RequestParam(required = false) String status) {

        // Lấy userId từ header (được set bởi proxy-client) hoặc từ JWT token
        String userIdHeader = request.getHeader("X-User-Id");
        UUID userId = null;

        // Nếu có X-User-Id header (từ proxy), dùng nó
        if (userIdHeader != null && !userIdHeader.isEmpty()) {
            try {
                userId = UUID.fromString(userIdHeader);
                log.info("📚 Using userId from X-User-Id header: {}", userId);
            } catch (IllegalArgumentException e) {
                log.error("❌ Invalid X-User-Id header format: {}", userIdHeader);
            }
        }

        // Nếu không có X-User-Id, thử lấy từ JWT token (direct call)
        if (userId == null) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String token = authHeader.substring(7);
                    if (jwtUtil.validateToken(token)) {
                        userId = jwtUtil.getUserIdFromToken(token);
                        log.info("📚 Using userId from JWT token: {}", userId);
                    } else {
                        log.error("❌ Invalid JWT token");
                    }
                } catch (Exception e) {
                    log.error("❌ Error parsing JWT token: {}", e.getMessage());
                }
            }
        }

        // Nếu vẫn không có userId, trả về lỗi
        if (userId == null) {
            log.error("❌ No user context found in headers for protected endpoint: {} {}",
                    request.getMethod(), request.getRequestURI());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "User not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }

        try {
            log.info("📚 Getting enrollments for user: {} with status filter: {}", userId, status);

            List<EnrollmentResponse> enrollments;

            if (status != null && !status.isEmpty()) {
                EnrollmentStatus enrollmentStatus = EnrollmentStatus.valueOf(status.toUpperCase());
                enrollments = enrollmentService.getUserEnrollmentsByStatus(userId, enrollmentStatus);
            } else {
                enrollments = enrollmentService.getUserEnrollments(userId);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("data", enrollments);
            response.put("total", enrollments.size());

            log.info("✅ Found {} enrollments for user: {}", enrollments.size(), userId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("❌ Invalid status or userId: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Invalid status or user ID");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            log.error("❌ Error getting user enrollments: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to get enrollments: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
