package com.techhub.app.paymentservice.controller;

import com.techhub.app.paymentservice.config.PayPalConfig;
import com.techhub.app.paymentservice.service.PayPalPaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

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
    public Map<String, Object> createOrder(@RequestParam Double amount) throws Exception {
        return payPalService.createOrder(amount, "USD");
    }

    @GetMapping("/success")
    public void success(@RequestParam String token,
                       @RequestParam(required = false) String PayerID,
                       HttpServletResponse response) throws IOException {
        try {
            // Capture payment from PayPal
            Map<String, Object> result = payPalService.captureOrder(token);

            log.info("PayPal payment captured successfully. Token: {}, PayerID: {}", token, PayerID);

            // Extract payment information
            String status = "success";
            String orderId = token;
            String amount = "N/A";

            // Try to extract amount from result
            if (result.containsKey("purchase_units") && result.get("purchase_units") instanceof java.util.List) {
                java.util.List<?> purchaseUnits = (java.util.List<?>) result.get("purchase_units");
                if (!purchaseUnits.isEmpty() && purchaseUnits.get(0) instanceof Map) {
                    Map<?, ?> unit = (Map<?, ?>) purchaseUnits.get(0);
                    if (unit.containsKey("amount") && unit.get("amount") instanceof Map) {
                        Map<?, ?> amountMap = (Map<?, ?>) unit.get("amount");
                        if (amountMap.containsKey("value")) {
                            amount = amountMap.get("value").toString();
                        }
                    }
                }
            }

            // Redirect to frontend with success status
            String redirectUrl = payPalConfig.getFrontendResultUrl() +
                    "?status=" + URLEncoder.encode(status, StandardCharsets.UTF_8) +
                    "&paymentMethod=" + URLEncoder.encode("PayPal", StandardCharsets.UTF_8) +
                    "&txnRef=" + URLEncoder.encode(orderId, StandardCharsets.UTF_8) +
                    "&amount=" + URLEncoder.encode(amount, StandardCharsets.UTF_8);

            log.info("Redirecting to frontend: {}", redirectUrl);
            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            log.error("Error processing PayPal success callback", e);

            // Redirect to frontend with error status
            String redirectUrl = payPalConfig.getFrontendResultUrl() +
                    "?status=" + URLEncoder.encode("failed", StandardCharsets.UTF_8) +
                    "&paymentMethod=" + URLEncoder.encode("PayPal", StandardCharsets.UTF_8) +
                    "&txnRef=" + URLEncoder.encode(token, StandardCharsets.UTF_8) +
                    "&message=" + URLEncoder.encode("Payment processing failed", StandardCharsets.UTF_8);

            response.sendRedirect(redirectUrl);
        }
    }

    @GetMapping("/cancel")
    public void cancel(@RequestParam(required = false) String token,
                      HttpServletResponse response) throws IOException {
        log.info("PayPal payment cancelled. Token: {}", token);

        // Redirect to frontend with cancelled status
        String redirectUrl = payPalConfig.getFrontendResultUrl() +
                "?status=" + URLEncoder.encode("cancelled", StandardCharsets.UTF_8) +
                "&paymentMethod=" + URLEncoder.encode("PayPal", StandardCharsets.UTF_8) +
                "&txnRef=" + URLEncoder.encode(token != null ? token : "N/A", StandardCharsets.UTF_8) +
                "&message=" + URLEncoder.encode("Payment was cancelled by user", StandardCharsets.UTF_8);

        log.info("Redirecting to frontend: {}", redirectUrl);
        response.sendRedirect(redirectUrl);
    }
}