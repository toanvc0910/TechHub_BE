package com.techhub.app.proxyclient.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "PAYMENT-SERVICE")
public interface PaymentServiceClient {

        // ===== PAYPAL ENDPOINTS =====

        @PostMapping("/api/v1/payment/paypal/create")
        ResponseEntity<String> createPayPalOrder(
                        @RequestParam(value = "amount", required = false) Double amount,
                        @RequestParam(value = "userId", required = false) String userId,
                        @RequestParam(value = "courseId", required = false) String courseId);

        @GetMapping("/api/v1/payment/paypal/capture")
        ResponseEntity<Map<String, String>> capturePayPalOrder(
                        @RequestParam("token") String token,
                        @RequestParam(value = "PayerID", required = false) String payerId);

        @GetMapping("/api/v1/payment/paypal/cancel-result")
        ResponseEntity<Map<String, String>> getPayPalCancelResult(
                        @RequestParam(value = "token", required = false) String token);

        // ===== VNPAY ENDPOINTS =====

        @GetMapping("/api/v1/payment/vn-pay")
        ResponseEntity<String> createVnPayPayment(
                        @RequestParam(value = "amount", required = false) String amount,
                        @RequestParam(value = "bankCode", required = false) String bankCode,
                        @RequestParam(value = "orderInfo", required = false) String orderInfo,
                        @RequestParam(value = "userId", required = false) String userId,
                        @RequestParam(value = "courseId", required = false) String courseId);

        @GetMapping("/api/v1/payment/vn-pay-callback-result")
        ResponseEntity<Map<String, String>> handleVnPayCallback(@SpringQueryMap Map<String, String> params);

        @GetMapping("/api/v1/fx/rate")
        ResponseEntity<String> getFxRate(
                        @RequestParam("from") String from,
                        @RequestParam("to") String to);

        @GetMapping("/api/v1/fx/convert")
        ResponseEntity<String> convertFx(
                        @RequestParam("from") String from,
                        @RequestParam("to") String to,
                        @RequestParam("amount") String amount);

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
                        @RequestHeader(value = "X-User-Roles", required = false) String roles,
                        @RequestHeader(value = "X-User-Id", required = false) String userId,
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
                        @RequestHeader(value = "X-User-Id", required = false) String userId,
                        @RequestParam(value = "instructorId", required = false) String instructorId,
                        @RequestParam(value = "courseId", required = false) String courseId,
                        @RequestParam(value = "refTime", required = false) String refTime);

        @GetMapping("/api/v1/payouts/balance")
        ResponseEntity<String> getPayoutBalance(
                        @RequestHeader(value = "X-User-Roles", required = false) String roles,
                        @RequestHeader(value = "X-User-Id", required = false) String userId,
                        @RequestParam(value = "instructorId", required = false) String instructorId);

        @PostMapping("/api/v1/payouts/requests")
        ResponseEntity<String> createPayoutRequest(
                        @RequestHeader(value = "X-User-Id", required = false) String userId,
                        @RequestBody Object payload);

        @GetMapping("/api/v1/payouts/requests")
        ResponseEntity<String> listPayoutRequests(
                        @RequestHeader(value = "X-User-Roles", required = false) String roles,
                        @RequestHeader(value = "X-User-Id", required = false) String userId);

        @GetMapping("/api/v1/payouts/summary")
        ResponseEntity<String> getPayoutOperationsSummary(
                        @RequestHeader(value = "X-User-Roles", required = false) String roles,
                        @RequestHeader(value = "X-User-Id", required = false) String userId);

        @GetMapping("/api/v1/payouts/requests/{requestId}")
        ResponseEntity<String> getPayoutRequest(
                        @RequestHeader(value = "X-User-Roles", required = false) String roles,
                        @RequestHeader(value = "X-User-Id", required = false) String userId,
                        @PathVariable("requestId") String requestId);

        @PutMapping("/api/v1/payouts/requests/{requestId}/approve")
        ResponseEntity<String> approvePayoutRequest(
                        @RequestHeader(value = "X-User-Roles", required = false) String roles,
                        @RequestHeader(value = "X-User-Id", required = false) String userId,
                        @PathVariable("requestId") String requestId,
                        @RequestBody(required = false) Object payload);

        @PutMapping("/api/v1/payouts/requests/{requestId}/settle")
        ResponseEntity<String> settleApprovedPayoutRequest(
                        @RequestHeader(value = "X-User-Roles", required = false) String roles,
                        @RequestHeader(value = "X-User-Id", required = false) String userId,
                        @PathVariable("requestId") String requestId,
                        @RequestBody(required = false) Object payload);

        @PutMapping("/api/v1/payouts/requests/{requestId}/reject")
        ResponseEntity<String> rejectPayoutRequest(
                        @RequestHeader(value = "X-User-Roles", required = false) String roles,
                        @RequestHeader(value = "X-User-Id", required = false) String userId,
                        @PathVariable("requestId") String requestId,
                        @RequestBody(required = false) Object payload);

        @PutMapping("/api/v1/payouts/requests/{requestId}/mark-paid")
        ResponseEntity<String> markPayoutRequestPaid(
                        @RequestHeader(value = "X-User-Roles", required = false) String roles,
                        @RequestHeader(value = "X-User-Id", required = false) String userId,
                        @PathVariable("requestId") String requestId,
                        @RequestBody Object payload);

        @GetMapping("/api/v1/payouts/batches")
        ResponseEntity<String> listPayoutBatches(
                        @RequestHeader(value = "X-User-Roles", required = false) String roles);

        @GetMapping("/api/v1/payouts/invoices")
        ResponseEntity<String> listPayoutInvoices(
                        @RequestHeader(value = "X-User-Roles", required = false) String roles,
                        @RequestHeader(value = "X-User-Id", required = false) String userId,
                        @RequestParam(value = "instructorId", required = false) String instructorId);

        @GetMapping("/api/v1/payouts/invoices/{invoiceId}")
        ResponseEntity<String> getPayoutInvoice(
                        @RequestHeader(value = "X-User-Roles", required = false) String roles,
                        @RequestHeader(value = "X-User-Id", required = false) String userId,
                        @PathVariable("invoiceId") String invoiceId);

        @GetMapping("/api/v1/payouts/invoices/{invoiceId}/pdf")
        ResponseEntity<byte[]> downloadPayoutInvoicePdf(
                        @RequestHeader(value = "X-User-Roles", required = false) String roles,
                        @RequestHeader(value = "X-User-Id", required = false) String userId,
                        @PathVariable("invoiceId") String invoiceId);

        @PostMapping("/api/v1/payouts/batches/monthly")
        ResponseEntity<String> createMonthlyPayoutBatch(
                        @RequestHeader(value = "X-User-Roles", required = false) String roles,
                        @RequestParam(value = "period", required = false) String period);

        @PostMapping("/api/v1/payouts/batches/manual")
        ResponseEntity<String> createManualPayoutBatch(
                        @RequestHeader(value = "X-User-Roles", required = false) String roles,
                        @RequestParam(value = "name", required = false) String name,
                        @RequestParam("fromDate") String fromDate,
                        @RequestParam("toDate") String toDate);
}
