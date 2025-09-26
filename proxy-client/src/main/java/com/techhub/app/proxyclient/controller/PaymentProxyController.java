package com.techhub.app.proxyclient.controller;

import com.techhub.app.proxyclient.client.PaymentServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/proxy/payments")
@RequiredArgsConstructor
public class PaymentProxyController {

    private final PaymentServiceClient paymentServiceClient;

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
