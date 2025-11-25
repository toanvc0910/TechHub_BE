package com.techhub.app.courseservice.controller;

import com.techhub.app.courseservice.dto.request.CreateEnrollmentRequest;
import com.techhub.app.courseservice.dto.response.EnrollmentResponse;
import com.techhub.app.courseservice.service.EnrollmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/enrollments")
@Slf4j
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createEnrollment(@Valid @RequestBody CreateEnrollmentRequest request) {
        log.info("Received request to create enrollment for user: {} and course: {}",
                request.getUserId(), request.getCourseId());

        try {
            EnrollmentResponse enrollment = enrollmentService.createEnrollment(request);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Enrollment created successfully");
            response.put("data", enrollment);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating enrollment: {}", e.getMessage(), e);

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
}

