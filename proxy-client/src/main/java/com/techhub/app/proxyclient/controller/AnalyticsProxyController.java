package com.techhub.app.proxyclient.controller;

import com.techhub.app.proxyclient.client.AnalyticsServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/proxy/analytics")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsProxyController {

    private final AnalyticsServiceClient analyticsServiceClient;

    @GetMapping("/instructor/overview")
    public ResponseEntity<String> getInstructorOverview(HttpServletRequest request,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        String userId = getRequiredUserId(request);
        log.info("[Analytics Proxy] GET /instructor/overview userId={} fromDate={} toDate={}", userId, fromDate,
                toDate);
        return analyticsServiceClient.getInstructorOverview(userId, fromDate, toDate);
    }

    @GetMapping("/admin/overview")
    public ResponseEntity<String> getAdminOverview(HttpServletRequest request,
            @RequestParam(required = false) String instructorId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        String roles = getRolesHeader(request);
        log.info("[Analytics Proxy] GET /admin/overview roles={} instructorId={} fromDate={} toDate={}", roles,
                instructorId, fromDate, toDate);
        return analyticsServiceClient.getAdminOverview(roles, instructorId, fromDate, toDate);
    }

    @GetMapping("/instructor/trends")
    public ResponseEntity<String> getInstructorTrends(HttpServletRequest request,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        String userId = getRequiredUserId(request);
        log.info("[Analytics Proxy] GET /instructor/trends userId={} fromDate={} toDate={}", userId, fromDate,
                toDate);
        return analyticsServiceClient.getInstructorTrends(userId, fromDate, toDate);
    }

    @GetMapping("/admin/trends")
    public ResponseEntity<String> getAdminTrends(HttpServletRequest request,
            @RequestParam(required = false) String instructorId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        String roles = getRolesHeader(request);
        log.info("[Analytics Proxy] GET /admin/trends roles={} instructorId={} fromDate={} toDate={}", roles,
                instructorId, fromDate, toDate);
        return analyticsServiceClient.getAdminTrends(roles, instructorId, fromDate, toDate);
    }

    private String getRequiredUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId == null) {
            log.warn("[Analytics Proxy] Missing userId in request context for {} {}", request.getMethod(),
                    request.getRequestURI());
            throw new IllegalStateException("Missing userId in request context");
        }
        return userId.toString();
    }

    private String getRolesHeader(HttpServletRequest request) {
        Object roles = request.getAttribute("userRoles");
        if (roles instanceof List<?>) {
            return ((List<?>) roles).stream().map(String::valueOf).collect(Collectors.joining(","));
        }
        return roles == null ? "" : roles.toString();
    }
}
