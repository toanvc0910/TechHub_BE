package com.techhub.app.analyticsservice.controller;

import com.techhub.app.analyticsservice.dto.RevenueOverviewResponse;
import com.techhub.app.analyticsservice.dto.RevenueDailyTrendResponse;
import com.techhub.app.analyticsservice.service.RevenueProjectionService;
import com.techhub.app.commonservice.payload.GlobalResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/analytics")
@RequiredArgsConstructor
@Slf4j
public class RevenueAnalyticsController {

    private final RevenueProjectionService projectionService;

    @GetMapping("/instructor/overview")
    public ResponseEntity<GlobalResponse<RevenueOverviewResponse>> getInstructorOverview(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate) {
        log.info("[Analytics BE] GET /instructor/overview X-User-Id={} fromDate={} toDate={}", userIdHeader,
                fromDate, toDate);
        UUID instructorId = UUID.fromString(userIdHeader);
        RevenueOverviewResponse response = projectionService.getInstructorOverview(instructorId, fromDate, toDate);
        return ResponseEntity.ok(GlobalResponse.success("Instructor revenue overview", response));
    }

    @GetMapping("/admin/overview")
    public ResponseEntity<GlobalResponse<RevenueOverviewResponse>> getAdminOverview(
            @RequestHeader(value = "X-User-Roles", required = false) String userRoles,
            @RequestParam(required = false) String instructorId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        log.info("[Analytics BE] GET /admin/overview X-User-Roles={} instructorId={} fromDate={} toDate={}",
                userRoles, instructorId, fromDate, toDate);
        if (!hasAdminRole(userRoles)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(GlobalResponse.error("Admin role is required", HttpStatus.FORBIDDEN.value()));
        }

        UUID parsedInstructorId;
        LocalDate parsedFromDate;
        LocalDate parsedToDate;
        try {
            parsedInstructorId = parseOptionalUuid(instructorId);
            parsedFromDate = parseOptionalDate(fromDate);
            parsedToDate = parseOptionalDate(toDate);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(GlobalResponse.error(ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
        }

        RevenueOverviewResponse response = projectionService.getAdminOverview(parsedInstructorId, parsedFromDate,
                parsedToDate);
        return ResponseEntity.ok(GlobalResponse.success("Admin revenue overview", response));
    }

    @GetMapping("/instructor/trends")
    public ResponseEntity<GlobalResponse<List<RevenueDailyTrendResponse>>> getInstructorTrend(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate) {
        log.info("[Analytics BE] GET /instructor/trends X-User-Id={} fromDate={} toDate={}", userIdHeader,
                fromDate, toDate);
        UUID instructorId = UUID.fromString(userIdHeader);
        List<RevenueDailyTrendResponse> response = projectionService.getInstructorTrend(instructorId, fromDate,
                toDate);
        return ResponseEntity.ok(GlobalResponse.success("Instructor revenue trend", response));
    }

    @GetMapping("/admin/trends")
    public ResponseEntity<GlobalResponse<List<RevenueDailyTrendResponse>>> getAdminTrend(
            @RequestHeader(value = "X-User-Roles", required = false) String userRoles,
            @RequestParam(required = false) String instructorId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        log.info("[Analytics BE] GET /admin/trends X-User-Roles={} instructorId={} fromDate={} toDate={}",
                userRoles, instructorId, fromDate, toDate);
        if (!hasAdminRole(userRoles)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(GlobalResponse.error("Admin role is required", HttpStatus.FORBIDDEN.value()));
        }

        UUID parsedInstructorId;
        LocalDate parsedFromDate;
        LocalDate parsedToDate;
        try {
            parsedInstructorId = parseOptionalUuid(instructorId);
            parsedFromDate = parseOptionalDate(fromDate);
            parsedToDate = parseOptionalDate(toDate);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(GlobalResponse.error(ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
        }

        List<RevenueDailyTrendResponse> response = projectionService.getAdminTrend(parsedInstructorId, parsedFromDate,
                parsedToDate);
        return ResponseEntity.ok(GlobalResponse.success("Admin revenue trend", response));
    }

    private UUID parseOptionalUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid instructorId format. Expected UUID.");
        }
    }

    private LocalDate parseOptionalDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String raw = value.trim();
        List<DateTimeFormatter> supported = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                DateTimeFormatter.ofPattern("M/d/yyyy"));

        for (DateTimeFormatter formatter : supported) {
            try {
                return LocalDate.parse(raw, formatter);
            } catch (DateTimeParseException ignored) {
                // try next format
            }
        }

        throw new IllegalArgumentException("Invalid date format. Expected yyyy-MM-dd or MM/dd/yyyy.");
    }

    private boolean hasAdminRole(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) {
            return false;
        }
        return rolesHeader.toUpperCase().contains("ADMIN");
    }
}
