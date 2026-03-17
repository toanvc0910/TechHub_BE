package com.techhub.app.courseservice.controller;

import com.techhub.app.commonservice.exception.BadRequestException;
import com.techhub.app.commonservice.exception.UnauthorizedException;
import com.techhub.app.commonservice.jwt.JwtUtil;
import com.techhub.app.commonservice.payload.GlobalResponse;
import com.techhub.app.courseservice.dto.request.CreateEnrollmentRequest;
import com.techhub.app.courseservice.dto.response.EnrollmentResponse;
import com.techhub.app.courseservice.enums.EnrollmentStatus;
import com.techhub.app.courseservice.service.EnrollmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
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
    public ResponseEntity<GlobalResponse<EnrollmentResponse>> createEnrollment(
            @Valid @RequestBody CreateEnrollmentRequest request,
            HttpServletRequest httpRequest) {
        log.info("💳 Received request to create enrollment for user: {} and course: {}",
                request.getUserId(), request.getCourseId());

        EnrollmentResponse enrollment = enrollmentService.createEnrollment(request);

        log.info("✅ Enrollment created successfully: {}", enrollment.getId());
        return ResponseEntity.status(201)
                .body(GlobalResponse.success("Enrollment created successfully", enrollment)
                        .withPath(httpRequest.getRequestURI()));
    }

    @GetMapping("/{enrollmentId}")
    public ResponseEntity<GlobalResponse<EnrollmentResponse>> getEnrollment(@PathVariable UUID enrollmentId,
            HttpServletRequest request) {
        log.info("Received request to get enrollment with ID: {}", enrollmentId);

        EnrollmentResponse enrollment = enrollmentService.getEnrollment(enrollmentId);

        return ResponseEntity.ok(
                GlobalResponse.success("Enrollment retrieved successfully", enrollment)
                        .withPath(request.getRequestURI()));
    }

    /**
     * Get current user's enrollments
     * Lấy danh sách khóa học mà user hiện tại đã enroll
     */
    @GetMapping("/my-enrollments")
    public ResponseEntity<GlobalResponse<List<EnrollmentResponse>>> getMyEnrollments(
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
                throw new BadRequestException("Invalid X-User-Id header format");
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
                        throw new UnauthorizedException("Invalid JWT token");
                    }
                } catch (RuntimeException e) {
                    throw new UnauthorizedException("Error parsing JWT token");
                }
            }
        }

        // Nếu vẫn không có userId, trả về lỗi
        if (userId == null) {
            log.error("❌ No user context found in headers for protected endpoint: {} {}",
                    request.getMethod(), request.getRequestURI());
            throw new UnauthorizedException("User not authenticated");
        }

        log.info("📚 Getting enrollments for user: {} with status filter: {}", userId, status);

        List<EnrollmentResponse> enrollments;

        if (status != null && !status.isEmpty()) {
            EnrollmentStatus enrollmentStatus = EnrollmentStatus.valueOf(status.toUpperCase());
            enrollments = enrollmentService.getUserEnrollmentsByStatus(userId, enrollmentStatus);
        } else {
            enrollments = enrollmentService.getUserEnrollments(userId);
        }

        log.info("✅ Found {} enrollments for user: {}", enrollments.size(), userId);
        return ResponseEntity.ok(
                GlobalResponse.success("Enrollments retrieved successfully", enrollments)
                        .withPath(request.getRequestURI()));
    }
}
