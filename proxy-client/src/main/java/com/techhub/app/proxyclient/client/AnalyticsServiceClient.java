package com.techhub.app.proxyclient.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "ANALYTICS-SERVICE")
public interface AnalyticsServiceClient {

        @GetMapping("/api/v1/analytics/instructor/overview")
        ResponseEntity<String> getInstructorOverview(
                        @RequestHeader("X-User-Id") String userId,
                        @RequestParam(value = "fromDate", required = false) String fromDate,
                        @RequestParam(value = "toDate", required = false) String toDate);

        @GetMapping("/api/v1/analytics/admin/overview")
        ResponseEntity<String> getAdminOverview(
                        @RequestHeader(value = "X-User-Roles", required = false) String roles,
                        @RequestParam(value = "instructorId", required = false) String instructorId,
                        @RequestParam(value = "fromDate", required = false) String fromDate,
                        @RequestParam(value = "toDate", required = false) String toDate);

        @GetMapping("/api/v1/analytics/instructor/trends")
        ResponseEntity<String> getInstructorTrends(
                        @RequestHeader("X-User-Id") String userId,
                        @RequestParam(value = "fromDate", required = false) String fromDate,
                        @RequestParam(value = "toDate", required = false) String toDate);

        @GetMapping("/api/v1/analytics/admin/trends")
        ResponseEntity<String> getAdminTrends(
                        @RequestHeader(value = "X-User-Roles", required = false) String roles,
                        @RequestParam(value = "instructorId", required = false) String instructorId,
                        @RequestParam(value = "fromDate", required = false) String fromDate,
                        @RequestParam(value = "toDate", required = false) String toDate);
}
