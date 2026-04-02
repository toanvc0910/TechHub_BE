package com.techhub.app.proxyclient.controller;

import com.techhub.app.proxyclient.client.AnalyticsServiceClient;
import lombok.RequiredArgsConstructor;
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
public class AnalyticsProxyController {

    private final AnalyticsServiceClient analyticsServiceClient;

    @GetMapping("/instructor/overview")
    public ResponseEntity<String> getInstructorOverview(HttpServletRequest request,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        String userId = getRequiredUserId(request);
        return analyticsServiceClient.getInstructorOverview(userId, fromDate, toDate);
    }

    @GetMapping("/admin/overview")
    public ResponseEntity<String> getAdminOverview(HttpServletRequest request,
            @RequestParam(required = false) String instructorId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        String roles = getRolesHeader(request);
        return analyticsServiceClient.getAdminOverview(roles, instructorId, fromDate, toDate);
    }

    private String getRequiredUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId == null) {
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
