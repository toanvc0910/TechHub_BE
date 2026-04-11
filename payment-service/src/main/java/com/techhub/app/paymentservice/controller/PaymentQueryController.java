package com.techhub.app.paymentservice.controller;

import com.techhub.app.commonservice.payload.GlobalResponse;
import com.techhub.app.paymentservice.dto.response.PaymentHistoryItemResponse;
import com.techhub.app.paymentservice.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentQueryController {

    private final TransactionService transactionService;

    @GetMapping("/history")
    public ResponseEntity<GlobalResponse<Page<PaymentHistoryItemResponse>>> getPaymentHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<PaymentHistoryItemResponse> history = transactionService.getPaymentHistory(page, size);
        return ResponseEntity.ok(GlobalResponse.success(history));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<GlobalResponse<PaymentHistoryItemResponse>> getPaymentById(@PathVariable UUID paymentId) {
        PaymentHistoryItemResponse payment = transactionService.getPaymentById(paymentId);
        return ResponseEntity.ok(GlobalResponse.success(payment));
    }
}
