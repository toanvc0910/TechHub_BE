package com.techhub.app.proxyclient.controller;

import com.techhub.app.proxyclient.client.PaymentServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("/api/proxy/payments")
@RequiredArgsConstructor
public class PaymentProxyController {

    private final PaymentServiceClient paymentServiceClient;

    // ===== PAYPAL ENDPOINTS =====

    @PostMapping("/paypal/create")
    public ResponseEntity<String> createPayPalOrder(@RequestParam Double amount) {
        return paymentServiceClient.createPayPalOrder(amount);
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
        response.sendRedirect("http://localhost:8084/api/v1/payment/paypal/success?" + queryParams);
    }

    @GetMapping("/paypal/cancel")
    public void paypalCancel(@RequestParam(required = false) String token,
                            HttpServletResponse response) throws IOException {
        String queryParams = token != null ? "token=" + token : "";
        response.sendRedirect("http://localhost:8084/api/v1/payment/paypal/cancel?" + queryParams);
    }

    // ===== VNPAY ENDPOINTS =====

    @GetMapping("/vn-pay")
    public ResponseEntity<String> createVnPayPayment(
            @RequestParam(value = "amount", required = false) String amount,
            @RequestParam(value = "bankCode", required = false) String bankCode,
            @RequestParam(value = "orderInfo", required = false) String orderInfo) {
        return paymentServiceClient.createVnPayPayment(amount, bankCode, orderInfo);
    }

    @GetMapping("/vn-pay-callback")
    public void vnPayCallback(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // VNPay callback redirects directly - we forward to the payment service callback URL
        // This is handled by VNPayPaymentController in payment-service
        String queryString = request.getQueryString();
        response.sendRedirect("http://localhost:8084/api/v1/payment/vn-pay-callback?" + queryString);
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
}
