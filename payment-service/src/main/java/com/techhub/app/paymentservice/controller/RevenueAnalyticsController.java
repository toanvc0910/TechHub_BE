package com.techhub.app.paymentservice.controller;

import com.techhub.app.commonservice.payload.GlobalResponse;
import com.techhub.app.paymentservice.dto.response.RevenueByCourseResponse;
import com.techhub.app.paymentservice.dto.response.RevenueOverviewResponse;
import com.techhub.app.paymentservice.service.RevenueAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/analytics")
@RequiredArgsConstructor
public class RevenueAnalyticsController {

    private final RevenueAnalyticsService revenueAnalyticsService;

    @GetMapping("/instructor/overview")
    public ResponseEntity<GlobalResponse<RevenueOverviewResponse>> getInstructorOverview(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate) {
        UUID instructorId = UUID.fromString(userIdHeader);
        RevenueOverviewResponse response = revenueAnalyticsService.getInstructorOverview(instructorId, fromDate,
                toDate);
        return ResponseEntity.ok(GlobalResponse.success("Instructor revenue overview", response));
    }

    @GetMapping("/instructor/courses")
    public ResponseEntity<GlobalResponse<List<RevenueByCourseResponse>>> getInstructorRevenueByCourse(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate) {
        UUID instructorId = UUID.fromString(userIdHeader);
        List<RevenueByCourseResponse> response = revenueAnalyticsService.getInstructorRevenueByCourse(instructorId,
                fromDate, toDate);
        return ResponseEntity.ok(GlobalResponse.success("Instructor revenue by course", response));
    }

    @GetMapping("/admin/overview")
    public ResponseEntity<GlobalResponse<RevenueOverviewResponse>> getAdminOverview(
            @RequestHeader(value = "X-User-Roles", required = false) String userRoles,
            @RequestParam(required = false) UUID instructorId,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate) {
        if (!hasAdminRole(userRoles)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(GlobalResponse.error("Admin role is required", HttpStatus.FORBIDDEN.value()));
        }

        RevenueOverviewResponse response = revenueAnalyticsService.getAdminOverview(instructorId, fromDate, toDate);
        return ResponseEntity.ok(GlobalResponse.success("Admin revenue overview", response));
    }

    private boolean hasAdminRole(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) {
            return false;
        }
        String normalized = rolesHeader.toUpperCase();
        return normalized.contains("ADMIN");
    }
}