package com.techhub.app.paymentservice.service;

import com.techhub.app.paymentservice.entity.Transaction;
import com.techhub.app.paymentservice.entity.TransactionItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class EnrollmentService {

    private final RestTemplate restTemplate;

    @Value("${course-service.url:http://localhost:8082}")
    private String courseServiceUrl;

    public EnrollmentService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Tạo enrollment cho user khi thanh toán thành công
     * @param transaction Transaction đã thanh toán thành công
     */
    public void createEnrollmentForTransaction(Transaction transaction) {
        if (transaction == null || transaction.getTransactionItems() == null || transaction.getTransactionItems().isEmpty()) {
            log.warn("Transaction is null or has no items, skipping enrollment creation");
            return;
        }

        UUID userId = transaction.getUserId();
        log.info("Creating enrollments for user: {} from transaction: {}", userId, transaction.getId());

        // Tạo enrollment cho mỗi course trong transaction
        for (TransactionItem item : transaction.getTransactionItems()) {
            try {
                createEnrollment(userId, item.getCourseId());
                log.info("Successfully created enrollment for user: {} and course: {}", userId, item.getCourseId());
            } catch (Exception e) {
                log.error("Failed to create enrollment for user: {} and course: {}. Error: {}",
                        userId, item.getCourseId(), e.getMessage(), e);
                // Continue processing other items even if one fails
            }
        }
    }

    /**
     * Gọi Course Service API để tạo enrollment
     * @param userId User ID
     * @param courseId Course ID
     */
    private void createEnrollment(UUID userId, UUID courseId) {
        String url = courseServiceUrl + "/api/v1/enrollments";

        // Tạo request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("userId", userId.toString());
        requestBody.put("courseId", courseId.toString());
        requestBody.put("status", "ENROLLED"); // Status mặc định khi vừa enroll

        // Tạo headers với inter-service authentication
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Add required headers for inter-service communication
        // These headers mimic proxy-client to bypass the UserContextInterceptor
        headers.set("X-Request-Source", "payment-service");
        headers.set("X-User-Id", userId.toString());
        headers.set("X-User-Email", "system@payment-service");
        headers.set("X-User-Roles", "SYSTEM");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            log.debug("Calling Course Service to create enrollment. URL: {}, Body: {}", url, requestBody);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Enrollment created successfully via Course Service. Response: {}", response.getBody());
            } else {
                log.error("Failed to create enrollment. Status: {}, Response: {}",
                        response.getStatusCode(), response.getBody());
                throw new RuntimeException("Course Service returned error status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error calling Course Service to create enrollment for user: {} and course: {}",
                    userId, courseId, e);
            throw new RuntimeException("Failed to create enrollment via Course Service", e);
        }
    }
}
