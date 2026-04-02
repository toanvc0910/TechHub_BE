package com.techhub.app.proxyclient.controller;

import com.techhub.app.proxyclient.client.PaymentServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/proxy/payments")
@RequiredArgsConstructor
public class PaymentProxyController {

    private final PaymentServiceClient paymentServiceClient;
    @Value("${PAYMENT_SERVICE_BASE_URL:http://localhost:8084}")
    private String paymentServiceBaseUrl;

    // ===== PAYPAL ENDPOINTS =====

    @PostMapping("/paypal/create")
    public ResponseEntity<String> createPayPalOrder(
            @RequestParam Double amount,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String courseId) {
        return paymentServiceClient.createPayPalOrder(amount, userId, courseId);
    }

    @GetMapping("/paypal/success")
    public void paypalSuccess(@RequestParam String token,
            @RequestParam(required = false) String PayerID,
            HttpServletResponse response) throws IOException {
        // Forward all parameters to payment service
        String queryParams = "token=" + token;
        if (PayerID != null) {
            queryParams += "&PayerID=" + PayerID;
        }
        response.sendRedirect(buildRedirectUrl("/api/v1/payment/paypal/success", queryParams));
    }

    @GetMapping("/paypal/cancel")
    public void paypalCancel(@RequestParam(required = false) String token,
            HttpServletResponse response) throws IOException {
        String queryParams = token != null ? "token=" + token : "";
        response.sendRedirect(buildRedirectUrl("/api/v1/payment/paypal/cancel", queryParams));
    }

    // ===== VNPAY ENDPOINTS =====

    @GetMapping("/vn-pay")
    public ResponseEntity<String> createVnPayPayment(
            @RequestParam(value = "amount", required = false) String amount,
            @RequestParam(value = "bankCode", required = false) String bankCode,
            @RequestParam(value = "orderInfo", required = false) String orderInfo,
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam(value = "courseId", required = false) String courseId) {
        return paymentServiceClient.createVnPayPayment(amount, bankCode, orderInfo, userId, courseId);
    }

    @GetMapping("/vn-pay-callback")
    public void vnPayCallback(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // VNPay callback redirects directly - we forward to the payment service
        // callback URL
        // This is handled by VNPayPaymentController in payment-service
        String queryString = request.getQueryString();
        response.sendRedirect(buildRedirectUrl("/api/v1/payment/vn-pay-callback", queryString));
    }

    private String buildRedirectUrl(String path, String query) {
        String normalizedBaseUrl = paymentServiceBaseUrl.endsWith("/")
                ? paymentServiceBaseUrl.substring(0, paymentServiceBaseUrl.length() - 1)
                : paymentServiceBaseUrl;

        if (query == null || query.isBlank()) {
            return normalizedBaseUrl + path;
        }
        return normalizedBaseUrl + path + "?" + query;
    }

    // ===== GENERIC PAYMENT ENDPOINTS =====

    @PostMapping("/create")
    public ResponseEntity<String> createPayment(@RequestBody Object paymentRequest,
            @RequestHeader("Authorization") String authHeader) {
        return paymentServiceClient.createPayment(paymentRequest, authHeader);
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<String> getPaymentStatus(@PathVariable String paymentId,
            @RequestHeader("Authorization") String authHeader) {
        return paymentServiceClient.getPaymentStatus(paymentId, authHeader);
    }

    @PostMapping("/callback/momo")
    public ResponseEntity<String> momoCallback(@RequestBody Object callbackData) {
        return paymentServiceClient.momoCallback(callbackData);
    }

    @PostMapping("/callback/zalopay")
    public ResponseEntity<String> zalopayCallback(@RequestBody Object callbackData) {
        return paymentServiceClient.zalopayCallback(callbackData);
    }

    @GetMapping("/history")
    public ResponseEntity<String> getPaymentHistory(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader("Authorization") String authHeader) {
        return paymentServiceClient.getPaymentHistory(page, size, authHeader);
    }

    @GetMapping("/analytics/instructor/overview")
    public ResponseEntity<String> getInstructorRevenueOverview(
            HttpServletRequest request,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        String userId = getRequiredUserId(request);
        return paymentServiceClient.getInstructorRevenueOverview(userId, fromDate, toDate);
    }

    @GetMapping("/analytics/instructor/courses")
    public ResponseEntity<String> getInstructorRevenueByCourse(
            HttpServletRequest request,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        String userId = getRequiredUserId(request);
        return paymentServiceClient.getInstructorRevenueByCourse(userId, fromDate, toDate);
    }

    @GetMapping("/analytics/admin/overview")
    public ResponseEntity<String> getAdminRevenueOverview(
            HttpServletRequest request,
            @RequestParam(required = false) String instructorId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        String roles = getRolesHeader(request);
        return paymentServiceClient.getAdminRevenueOverview(roles, instructorId, fromDate, toDate);
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
