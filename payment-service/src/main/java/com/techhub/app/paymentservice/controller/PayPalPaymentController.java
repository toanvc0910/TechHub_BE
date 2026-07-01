package com.techhub.app.paymentservice.controller;

import com.techhub.app.commonservice.payload.GlobalResponse;
import com.techhub.app.paymentservice.config.PayPalConfig;
import com.techhub.app.paymentservice.service.PayPalPaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("api/v1/payment/paypal")
public class PayPalPaymentController {

    private final PayPalPaymentService payPalService;
    private final PayPalConfig payPalConfig;

    public PayPalPaymentController(PayPalPaymentService payPalService, PayPalConfig payPalConfig) {
        this.payPalService = payPalService;
        this.payPalConfig = payPalConfig;
    }

    @PostMapping("/create")
    public ResponseEntity<GlobalResponse<Map<String, Object>>> createOrder(
            @RequestParam(required = false) Double amount,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String courseId,
            javax.servlet.http.HttpServletRequest request) throws Exception {
        log.info("Creating PayPal order for userId: {}, courseId: {}; client amount ignored={}",
                userId, courseId, amount);

        UUID userUUID = null;
        if (userId != null && !userId.isEmpty()) {
            try {
                userUUID = UUID.fromString(userId);
            } catch (IllegalArgumentException e) {
                log.error("Invalid userId format: {}", userId);
                throw new IllegalArgumentException("Invalid userId format: " + userId);
            }
        }

        // Nếu không có userId, throw exception yêu cầu phải có userId
        if (userUUID == null) {
            log.error("PayPal payment requires userId parameter");
            throw new IllegalArgumentException("userId parameter is required for PayPal payment");
        }

        // Validate courseId
        UUID courseUUID = null;
        if (courseId != null && !courseId.isEmpty()) {
            try {
                courseUUID = UUID.fromString(courseId);
            } catch (IllegalArgumentException e) {
                log.error("Invalid courseId format: {}", courseId);
                throw new IllegalArgumentException("Invalid courseId format: " + courseId);
            }
        }

        // Nếu không có courseId, throw exception yêu cầu phải có courseId
        if (courseUUID == null) {
            log.error("PayPal payment requires courseId parameter");
            throw new IllegalArgumentException("courseId parameter is required for PayPal payment");
        }

        Map<String, Object> order = payPalService.createOrder(amount, null, userUUID, courseUUID);
        return ResponseEntity.ok(
                GlobalResponse.success("PayPal order created", order)
                        .withPath(request.getRequestURI()));
    }

    @GetMapping("/success")
    public void success(@RequestParam String token,
            @RequestParam(required = false) String PayerID,
            HttpServletResponse response) throws IOException {
        log.info("Received PayPal success callback. token={}, payerId={}", token, PayerID);
        response.sendRedirect(buildFrontendRedirectUrl(captureResult(token, PayerID)));
    }

    @GetMapping("/cancel")
    public void cancel(@RequestParam(required = false) String token,
            HttpServletResponse response) throws IOException {
        log.info("PayPal payment cancelled. Token: {}", token);
        response.sendRedirect(buildFrontendRedirectUrl(cancelResult(token)));
    }

    @GetMapping("/capture")
    public ResponseEntity<Map<String, String>> capture(@RequestParam String token,
            @RequestParam(required = false) String PayerID) {
        return ResponseEntity.ok(captureResult(token, PayerID));
    }

    @GetMapping("/cancel-result")
    public ResponseEntity<Map<String, String>> cancelResultEndpoint(@RequestParam(required = false) String token) {
        return ResponseEntity.ok(cancelResult(token));
    }

    private Map<String, String> captureResult(String token, String payerId) {
        try {
            Map<String, Object> result = payPalService.captureOrder(token);
            String amount = extractAmount(result);

            log.info("PayPal payment captured successfully. token={}, payerId={}, amount={}", token, payerId, amount);

            Map<String, String> params = new LinkedHashMap<>();
            params.put("status", "success");
            params.put("paymentMethod", "PayPal");
            params.put("txnRef", token);
            params.put("amount", amount);
            return params;
        } catch (Exception e) {
            log.error("Error processing PayPal success callback", e);

            Map<String, String> params = new LinkedHashMap<>();
            params.put("status", "failed");
            params.put("paymentMethod", "PayPal");
            params.put("txnRef", token);
            params.put("message", "Payment processing failed");
            return params;
        }
    }

    private Map<String, String> cancelResult(String token) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("status", "cancelled");
        params.put("paymentMethod", "PayPal");
        params.put("txnRef", token != null ? token : "N/A");
        params.put("message", "Payment was cancelled by user");
        return params;
    }

    private String extractAmount(Map<String, Object> result) {
        if (result.containsKey("purchase_units") && result.get("purchase_units") instanceof java.util.List) {
            java.util.List<?> purchaseUnits = (java.util.List<?>) result.get("purchase_units");
            if (!purchaseUnits.isEmpty() && purchaseUnits.get(0) instanceof Map) {
                Map<?, ?> unit = (Map<?, ?>) purchaseUnits.get(0);
                if (unit.containsKey("amount") && unit.get("amount") instanceof Map) {
                    Map<?, ?> amountMap = (Map<?, ?>) unit.get("amount");
                    if (amountMap.containsKey("value")) {
                        return amountMap.get("value").toString();
                    }
                }
            }
        }
        return "N/A";
    }

    private String buildFrontendRedirectUrl(Map<String, String> params) {
        StringBuilder redirectUrl = new StringBuilder(payPalConfig.getFrontendResultUrl());
        redirectUrl.append("?");

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

        log.info("Redirecting to frontend: {}", redirectUrl);
        return redirectUrl.toString();
    }
}
