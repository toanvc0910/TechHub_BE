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
        @RequestParam(value = "userId", required = false) String userId
    );

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
        @RequestParam(value = "userId", required = false) String userId
    );

    // Note: VNPay callback is handled directly in payment-service, not through Feign
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
}
