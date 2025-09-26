package com.techhub.app.proxyclient.client;

import com.techhub.app.proxyclient.constant.AppConstant;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "PAYMENT-SERVICE")
public interface PaymentServiceClient {

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
