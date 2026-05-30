package com.techhub.app.proxyclient.controller;

import com.techhub.app.proxyclient.client.AnalyticsServiceClient;
import com.techhub.app.proxyclient.client.PaymentServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/proxy/payments")
@RequiredArgsConstructor
public class PaymentProxyController {

    private final PaymentServiceClient paymentServiceClient;
    private final AnalyticsServiceClient analyticsServiceClient;
    @Value("${PAYMENT_FRONTEND_RESULT_URL}")
    private String paymentFrontendResultUrl;
    @Value("${PAYMENT_FRONTEND_VNPAY_RETURN_URL:http://localhost:3000/vnpay-return}")
    private String paymentFrontendVnpayReturnUrl;

    // ===== PAYPAL ENDPOINTS =====

    @PostMapping("/paypal/create")
    public ResponseEntity<String> createPayPalOrder(
            @RequestParam(required = false) Double amount,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String courseId) {
        return paymentServiceClient.createPayPalOrder(amount, userId, courseId);
    }

    @GetMapping("/paypal/success")
    public void paypalSuccess(@RequestParam String token,
            @RequestParam(required = false) String PayerID,
            HttpServletResponse response) throws IOException {
        Map<String, String> result = paymentServiceClient.capturePayPalOrder(token, PayerID).getBody();
        response.sendRedirect(buildFrontendResultUrl(paymentFrontendResultUrl, result, failedPayPalResult(token)));
    }

    @GetMapping("/paypal/cancel")
    public void paypalCancel(@RequestParam(required = false) String token,
            HttpServletResponse response) throws IOException {
        Map<String, String> result = paymentServiceClient.getPayPalCancelResult(token).getBody();
        response.sendRedirect(buildFrontendResultUrl(paymentFrontendResultUrl, result, cancelledPayPalResult(token)));
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
        Map<String, String> params = request.getParameterMap().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue()[0]));
        Map<String, String> result = paymentServiceClient.handleVnPayCallback(params).getBody();
        response.sendRedirect(buildFrontendResultUrl(paymentFrontendVnpayReturnUrl, result, failedVnPayResult()));
    }

    @GetMapping("/fx/rate")
    public ResponseEntity<String> getFxRate(@RequestParam String from, @RequestParam String to) {
        return paymentServiceClient.getFxRate(from, to);
    }

    @GetMapping("/fx/convert")
    public ResponseEntity<String> convertFx(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam String amount) {
        return paymentServiceClient.convertFx(from, to, amount);
    }

    private String buildFrontendResultUrl(String frontendResultUrl, Map<String, String> result,
            Map<String, String> fallback) {
        Map<String, String> params = result == null || result.isEmpty() ? fallback : result;
        StringBuilder redirectUrl = new StringBuilder(frontendResultUrl);
        redirectUrl.append(frontendResultUrl.contains("?") ? "&" : "?");

        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) {
                redirectUrl.append("&");
            }
            redirectUrl.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            redirectUrl.append("=");
            redirectUrl.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            first = false;
        }

        return redirectUrl.toString();
    }

    private Map<String, String> failedPayPalResult(String token) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("status", "failed");
        params.put("paymentMethod", "PayPal");
        params.put("txnRef", token);
        params.put("message", "Payment processing failed");
        return params;
    }

    private Map<String, String> cancelledPayPalResult(String token) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("status", "cancelled");
        params.put("paymentMethod", "PayPal");
        params.put("txnRef", token != null ? token : "N/A");
        params.put("message", "Payment was cancelled by user");
        return params;
    }

    private Map<String, String> failedVnPayResult() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("status", "failed");
        params.put("paymentMethod", "VNPay");
        params.put("txnRef", "N/A");
        params.put("amount", "0");
        return params;
    }

    // ===== GENERIC PAYMENT ENDPOINTS =====

    @PostMapping("/create")
    public ResponseEntity<String> createPayment(@RequestBody Object paymentRequest,
            @RequestHeader("Authorization") String authHeader) {
        return paymentServiceClient.createPayment(paymentRequest, authHeader);
    }

    @GetMapping("/{paymentId:[0-9a-fA-F-]{36}}")
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
            HttpServletRequest request,
            @RequestHeader("Authorization") String authHeader) {
        return paymentServiceClient.getPaymentHistory(page, size, getRolesHeader(request), getUserIdHeader(request),
                authHeader);
    }

    @GetMapping("/analytics/instructor/overview")
    public ResponseEntity<String> getInstructorRevenueOverview(HttpServletRequest request,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        return analyticsServiceClient.getInstructorOverview(getRequiredUserId(request), fromDate, toDate);
    }

    @GetMapping("/analytics/instructor/trends")
    public ResponseEntity<String> getInstructorRevenueTrends(HttpServletRequest request,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        return analyticsServiceClient.getInstructorTrends(getRequiredUserId(request), fromDate, toDate);
    }

    @GetMapping("/analytics/admin/overview")
    public ResponseEntity<String> getAdminRevenueOverview(HttpServletRequest request,
            @RequestParam(required = false) String instructorId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        return analyticsServiceClient.getAdminOverview(getRolesHeader(request), instructorId, fromDate, toDate);
    }

    @GetMapping("/analytics/admin/trends")
    public ResponseEntity<String> getAdminRevenueTrends(HttpServletRequest request,
            @RequestParam(required = false) String instructorId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        return analyticsServiceClient.getAdminTrends(getRolesHeader(request), instructorId, fromDate, toDate);
    }

    @PostMapping("/revenue-policies")
    public ResponseEntity<String> createRevenuePolicy(HttpServletRequest request,
            @RequestBody Object payload) {
        return paymentServiceClient.createRevenuePolicy(getRolesHeader(request), payload);
    }

    @GetMapping("/revenue-policies")
    public ResponseEntity<String> listRevenuePolicies(HttpServletRequest request,
            @RequestParam(required = false) String scope) {
        return paymentServiceClient.listRevenuePolicies(getRolesHeader(request), scope);
    }

    @GetMapping("/revenue-policies/active")
    public ResponseEntity<String> getActiveRevenuePolicy(HttpServletRequest request,
            @RequestParam(required = false) String instructorId,
            @RequestParam(required = false) String courseId,
            @RequestParam(required = false) String refTime) {
        return paymentServiceClient.getActiveRevenuePolicy(
                getRolesHeader(request), getUserIdHeader(request), instructorId, courseId, refTime);
    }

    @GetMapping("/payouts/balance")
    public ResponseEntity<String> getPayoutBalance(HttpServletRequest request,
            @RequestParam(required = false) String instructorId) {
        return paymentServiceClient.getPayoutBalance(getRolesHeader(request), getUserIdHeader(request), instructorId);
    }

    @PostMapping("/payouts/requests")
    public ResponseEntity<String> createPayoutRequest(HttpServletRequest request,
            @RequestBody Object payload) {
        return paymentServiceClient.createPayoutRequest(getUserIdHeader(request), payload);
    }

    @GetMapping("/payouts/requests")
    public ResponseEntity<String> listPayoutRequests(HttpServletRequest request) {
        return paymentServiceClient.listPayoutRequests(getRolesHeader(request), getUserIdHeader(request));
    }

    @GetMapping("/payouts/requests/{requestId}")
    public ResponseEntity<String> getPayoutRequest(HttpServletRequest request,
            @PathVariable String requestId) {
        return paymentServiceClient.getPayoutRequest(getRolesHeader(request), getUserIdHeader(request), requestId);
    }

    @PutMapping("/payouts/requests/{requestId}/approve")
    public ResponseEntity<String> approvePayoutRequest(HttpServletRequest request,
            @PathVariable String requestId,
            @RequestBody(required = false) Object payload) {
        return paymentServiceClient.approvePayoutRequest(getRolesHeader(request), getUserIdHeader(request), requestId,
                payload);
    }

    @PutMapping("/payouts/requests/{requestId}/settle")
    public ResponseEntity<String> settleApprovedPayoutRequest(HttpServletRequest request,
            @PathVariable String requestId,
            @RequestBody(required = false) Object payload) {
        return paymentServiceClient.settleApprovedPayoutRequest(getRolesHeader(request), getUserIdHeader(request),
                requestId, payload);
    }

    @PutMapping("/payouts/requests/{requestId}/reject")
    public ResponseEntity<String> rejectPayoutRequest(HttpServletRequest request,
            @PathVariable String requestId,
            @RequestBody(required = false) Object payload) {
        return paymentServiceClient.rejectPayoutRequest(getRolesHeader(request), getUserIdHeader(request), requestId,
                payload);
    }

    @PutMapping("/payouts/requests/{requestId}/mark-paid")
    public ResponseEntity<String> markPayoutRequestPaid(HttpServletRequest request,
            @PathVariable String requestId,
            @RequestBody Object payload) {
        return paymentServiceClient.markPayoutRequestPaid(getRolesHeader(request), getUserIdHeader(request),
                requestId,
                payload);
    }

    @GetMapping("/payouts/batches")
    public ResponseEntity<String> listPayoutBatches(HttpServletRequest request) {
        return paymentServiceClient.listPayoutBatches(getRolesHeader(request));
    }

    @GetMapping("/payouts/invoices")
    public ResponseEntity<String> listPayoutInvoices(HttpServletRequest request,
            @RequestParam(required = false) String instructorId) {
        return paymentServiceClient.listPayoutInvoices(getRolesHeader(request), getUserIdHeader(request),
                instructorId);
    }

    @GetMapping("/payouts/invoices/{invoiceId}")
    public ResponseEntity<String> getPayoutInvoice(HttpServletRequest request,
            @PathVariable String invoiceId) {
        return paymentServiceClient.getPayoutInvoice(getRolesHeader(request), getUserIdHeader(request), invoiceId);
    }

    @GetMapping("/payouts/invoices/{invoiceId}/pdf")
    public ResponseEntity<byte[]> downloadPayoutInvoicePdf(HttpServletRequest request,
            @PathVariable String invoiceId) {
        return paymentServiceClient.downloadPayoutInvoicePdf(getRolesHeader(request), getUserIdHeader(request),
                invoiceId);
    }

    @PostMapping("/payouts/batches/monthly")
    public ResponseEntity<String> createMonthlyPayoutBatch(HttpServletRequest request,
            @RequestParam(required = false) String period) {
        return paymentServiceClient.createMonthlyPayoutBatch(getRolesHeader(request), period);
    }

    @PostMapping("/payouts/batches/manual")
    public ResponseEntity<String> createManualPayoutBatch(HttpServletRequest request,
            @RequestParam(required = false) String name,
            @RequestParam String fromDate,
            @RequestParam String toDate) {
        return paymentServiceClient.createManualPayoutBatch(getRolesHeader(request), name, fromDate, toDate);
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
        return roles == null ? "" : roles.toString();
    }

    private String getUserIdHeader(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        return userId == null ? "" : userId.toString();
    }
}
