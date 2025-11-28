package com.techhub.app.paymentservice.service;

import com.techhub.app.paymentservice.entity.Transaction;
import com.techhub.app.paymentservice.entity.TransactionItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class EnrollmentService {

    private final RestTemplate restTemplate;

    // Dùng service name từ Eureka thay vì hardcode URL
    @Value("${course-service.name:COURSE-SERVICE}")
    private String courseServiceName;

    public EnrollmentService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Tạo enrollment cho user khi thanh toán thành công
     * 
     * @param transaction Transaction đã thanh toán thành công
     */
    public void createEnrollmentForTransaction(Transaction transaction) {
        if (transaction == null || transaction.getTransactionItems() == null
                || transaction.getTransactionItems().isEmpty()) {
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
            }
        }
    }

    /**
     * Gọi Course Service API để tạo enrollment với retry logic
     * 
     * @param userId   User ID
     * @param courseId Course ID
     */
    private void createEnrollment(UUID userId, UUID courseId) {
        // Eureka sẽ tự động resolve service name thành actual URL
        String url = "http://" + courseServiceName + "/api/enrollments";

        log.info("🌐 Using Service Discovery - Service: {}, Endpoint: {}", courseServiceName, url); // Tạo request body
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

        // Retry logic: tối đa 3 lần
        int maxRetries = 3;
        int retryCount = 0;
        Exception lastException = null;

        while (retryCount < maxRetries) {
            try {
                log.info(
                        "Calling Course Service to create enrollment (attempt {}/{}). URL: {}, userId: {}, courseId: {}",
                        retryCount + 1, maxRetries, url, userId, courseId);

                ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("✅ Enrollment created successfully for user: {} and course: {}. Response: {}",
                            userId, courseId, response.getBody());
                    return; // Success - exit method
                } else {
                    log.error("❌ Course Service returned non-2xx status: {}, Response: {}",
                            response.getStatusCode(), response.getBody());
                    lastException = new RuntimeException(
                            "Course Service returned error status: " + response.getStatusCode());
                }
            } catch (HttpClientErrorException e) {
                // 4xx errors - client error, không retry
                log.error("❌ Client error when creating enrollment (user: {}, course: {}). Status: {}, Response: {}",
                        userId, courseId, e.getStatusCode(), e.getResponseBodyAsString());

                // Nếu là 409 Conflict (đã enroll rồi), coi như thành công
                if (e.getStatusCode() == HttpStatus.CONFLICT) {
                    log.info("User {} already enrolled in course {}, treating as success", userId, courseId);
                    return;
                }

                throw new RuntimeException("Failed to create enrollment: " + e.getMessage(), e);
            } catch (HttpServerErrorException e) {
                // 5xx errors - server error, có thể retry
                log.warn("⚠️ Server error when creating enrollment (attempt {}/{}). Status: {}, Response: {}",
                        retryCount + 1, maxRetries, e.getStatusCode(), e.getResponseBodyAsString());
                lastException = e;
            } catch (Exception e) {
                // Network errors, timeout, etc - có thể retry
                log.warn("⚠️ Error calling Course Service (attempt {}/{}). Error: {}",
                        retryCount + 1, maxRetries, e.getMessage());
                lastException = e;
            }

            retryCount++;

            // Đợi trước khi retry (exponential backoff)
            if (retryCount < maxRetries) {
                try {
                    long waitTime = (long) Math.pow(2, retryCount) * 1000; // 2s, 4s, 8s
                    log.info("Waiting {}ms before retry...", waitTime);
                    Thread.sleep(waitTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting to retry", ie);
                }
            }
        }

        // Đã retry hết số lần cho phép
        log.error("❌ Failed to create enrollment after {} attempts for user: {} and course: {}",
                maxRetries, userId, courseId);
        throw new RuntimeException("Failed to create enrollment via Course Service after " + maxRetries + " attempts",
                lastException);
    }
}
