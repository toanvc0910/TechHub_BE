package com.techhub.app.proxyclient.controller;

import com.techhub.app.proxyclient.client.CourseServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
