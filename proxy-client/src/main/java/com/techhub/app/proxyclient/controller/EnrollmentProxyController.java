package com.techhub.app.proxyclient.controller;

import com.techhub.app.proxyclient.client.CourseServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Proxy controller for enrollment-related endpoints
 * Forwards requests to Course Service
 */
@Slf4j
@RestController
@RequestMapping("/api/proxy/enrollments")
@RequiredArgsConstructor
public class EnrollmentProxyController {

    private final CourseServiceClient courseServiceClient;

    /**
     * Create enrollment
     * 
     * @param request    Enrollment creation request
     * @param authHeader Authorization header with JWT token
     * @return Created enrollment
     */
    @PostMapping
    public ResponseEntity<String> createEnrollment(
            @RequestBody Object request,
            @RequestHeader("Authorization") String authHeader) {

        log.info("💳 Proxying request to create enrollment");
        return courseServiceClient.createEnrollment(request, authHeader);
    }

    /**
     * Get enrollment by ID
     * 
     * @param enrollmentId Enrollment ID
     * @param authHeader   Authorization header with JWT token
     * @return Enrollment details
     */
    @GetMapping("/{enrollmentId}")
    public ResponseEntity<String> getEnrollment(
            @PathVariable String enrollmentId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        log.info("📋 Proxying request to get enrollment: {}", enrollmentId);
        return courseServiceClient.getEnrollment(enrollmentId, authHeader);
    }

    /**
     * Get current user's enrollments (My Learning)
     * 
     * @param status     Optional filter by enrollment status (ENROLLED,
     *                   IN_PROGRESS, COMPLETED, DROPPED)
     * @param authHeader Authorization header with JWT token
     * @return List of user's enrollments
     */
    @GetMapping("/my-enrollments")
    public ResponseEntity<String> getMyEnrollments(
            @RequestParam(required = false) String status,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        log.info("📚 Proxying request to get user enrollments with status: {}", status);
        return courseServiceClient.getMyEnrollments(status, authHeader);
    }
}
