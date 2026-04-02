package com.techhub.app.analyticsservice.controller;

import com.techhub.app.analyticsservice.dto.RevenueOverviewResponse;
import com.techhub.app.analyticsservice.dto.RevenueDailyTrendResponse;
import com.techhub.app.analyticsservice.service.RevenueProjectionService;
import com.techhub.app.commonservice.payload.GlobalResponse;
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

    private final RevenueProjectionService projectionService;

    @GetMapping("/instructor/overview")
    public ResponseEntity<GlobalResponse<RevenueOverviewResponse>> getInstructorOverview(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate) {
        UUID instructorId = UUID.fromString(userIdHeader);
        RevenueOverviewResponse response = projectionService.getInstructorOverview(instructorId, fromDate, toDate);
        return ResponseEntity.ok(GlobalResponse.success("Instructor revenue overview", response));
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

        RevenueOverviewResponse response = projectionService.getAdminOverview(instructorId, fromDate, toDate);
        return ResponseEntity.ok(GlobalResponse.success("Admin revenue overview", response));
    }

    @GetMapping("/instructor/trends")
    public ResponseEntity<GlobalResponse<List<RevenueDailyTrendResponse>>> getInstructorTrend(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate) {
        UUID instructorId = UUID.fromString(userIdHeader);
        List<RevenueDailyTrendResponse> response = projectionService.getInstructorTrend(instructorId, fromDate,
                toDate);
        return ResponseEntity.ok(GlobalResponse.success("Instructor revenue trend", response));
    }

    @GetMapping("/admin/trends")
    public ResponseEntity<GlobalResponse<List<RevenueDailyTrendResponse>>> getAdminTrend(
            @RequestHeader(value = "X-User-Roles", required = false) String userRoles,
            @RequestParam(required = false) UUID instructorId,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate) {
        if (!hasAdminRole(userRoles)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(GlobalResponse.error("Admin role is required", HttpStatus.FORBIDDEN.value()));
        }

        List<RevenueDailyTrendResponse> response = projectionService.getAdminTrend(instructorId, fromDate, toDate);
        return ResponseEntity.ok(GlobalResponse.success("Admin revenue trend", response));
    }

    private boolean hasAdminRole(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) {
            return false;
        }
        return rolesHeader.toUpperCase().contains("ADMIN");
    }
}
