package com.techhub.app.proxyclient.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "PAYMENT-SERVICE")
public interface PaymentServiceClient {

        // ===== PAYPAL ENDPOINTS =====

        @PostMapping("/api/v1/payment/paypal/create")
        ResponseEntity<String> createPayPalOrder(
                        @RequestParam("amount") Double amount,
                        @RequestParam(value = "userId", required = false) String userId,
                        @RequestParam(value = "courseId", required = false) String courseId);

        @GetMapping("/api/v1/payment/paypal/success")
        ResponseEntity<String> paypalSuccess(@RequestParam("token") String token);

        @GetMapping("/api/v1/payment/paypal/cancel")
        ResponseEntity<String> paypalCancel();

        // ===== VNPAY ENDPOINTS =====

        @GetMapping("/api/v1/payment/vn-pay")
        ResponseEntity<String> createVnPayPayment(
                        @RequestParam(value = "amount", required = false) String amount,
                        @RequestParam(value = "bankCode", required = false) String bankCode,
                        @RequestParam(value = "orderInfo", required = false) String orderInfo,
                        @RequestParam(value = "userId", required = false) String userId,
                        @RequestParam(value = "courseId", required = false) String courseId);

        // Note: VNPay callback is handled directly in payment-service, not through
        // Feign
        // The callback endpoint in proxy controller will forward the request parameters

        // ===== GENERIC PAYMENT ENDPOINTS =====

        @PostMapping("/api/payments/create")
        ResponseEntity<String> createPayment(@RequestBody Object paymentRequest,
                        @RequestHeader("Authorization") String authHeader);

        @GetMapping("/api/payments/{paymentId}")
        ResponseEntity<String> getPaymentStatus(@PathVariable String paymentId,
                        @RequestHeader("Authorization") String authHeader);

        @PostMapping("/api/payments/callback/momo")
        ResponseEntity<String> momoCallback(@RequestBody Object callbackData);

        @PostMapping("/api/payments/callback/zalopay")
        ResponseEntity<String> zalopayCallback(@RequestBody Object callbackData);

        @GetMapping("/api/payments/history")
        ResponseEntity<String> getPaymentHistory(@RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestHeader("Authorization") String authHeader);

        @GetMapping("/api/v1/analytics/instructor/overview")
        ResponseEntity<String> getInstructorRevenueOverview(
                        @RequestHeader("X-User-Id") String userId,
                        @RequestParam(value = "fromDate", required = false) String fromDate,
                        @RequestParam(value = "toDate", required = false) String toDate);

        @GetMapping("/api/v1/analytics/instructor/trends")
        ResponseEntity<String> getInstructorRevenueTrends(
                        @RequestHeader("X-User-Id") String userId,
                        @RequestParam(value = "fromDate", required = false) String fromDate,
                        @RequestParam(value = "toDate", required = false) String toDate);

        @GetMapping("/api/v1/analytics/admin/overview")
        ResponseEntity<String> getAdminRevenueOverview(
                        @RequestHeader(value = "X-User-Roles", required = false) String roles,
                        @RequestParam(value = "instructorId", required = false) String instructorId,
                        @RequestParam(value = "fromDate", required = false) String fromDate,
                        @RequestParam(value = "toDate", required = false) String toDate);

        @GetMapping("/api/v1/analytics/admin/trends")
        ResponseEntity<String> getAdminRevenueTrends(
                        @RequestHeader(value = "X-User-Roles", required = false) String roles,
                        @RequestParam(value = "instructorId", required = false) String instructorId,
                        @RequestParam(value = "fromDate", required = false) String fromDate,
                        @RequestParam(value = "toDate", required = false) String toDate);

        @PostMapping("/api/v1/revenue-policies")
        ResponseEntity<String> createRevenuePolicy(
                        @RequestHeader(value = "X-User-Roles", required = false) String roles,
                        @RequestBody Object request);

        @GetMapping("/api/v1/revenue-policies")
        ResponseEntity<String> listRevenuePolicies(
                        @RequestHeader(value = "X-User-Roles", required = false) String roles,
                        @RequestParam(value = "scope", required = false) String scope);

        @GetMapping("/api/v1/revenue-policies/active")
        ResponseEntity<String> getActiveRevenuePolicy(
                        @RequestHeader(value = "X-User-Roles", required = false) String roles,
                        @RequestParam(value = "instructorId", required = false) String instructorId,
                        @RequestParam(value = "courseId", required = false) String courseId,
                        @RequestParam(value = "refTime", required = false) String refTime);
}
