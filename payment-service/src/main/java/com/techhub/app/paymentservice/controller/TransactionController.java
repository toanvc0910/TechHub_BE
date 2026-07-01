package com.techhub.app.paymentservice.controller;

import com.techhub.app.commonservice.payload.GlobalResponse;
import com.techhub.app.paymentservice.entity.Payment;
import com.techhub.app.paymentservice.entity.Transaction;
import com.techhub.app.paymentservice.entity.enums.TransactionStatus;
import com.techhub.app.paymentservice.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<GlobalResponse<List<Transaction>>> getTransactionsByUserId(@PathVariable UUID userId) {
        List<Transaction> transactions = transactionService.getTransactionsByUserId(userId);
        return ResponseEntity.ok(GlobalResponse.success(transactions));
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<GlobalResponse<Transaction>> getTransactionById(@PathVariable UUID transactionId) {
        Transaction transaction = transactionService.getTransactionById(transactionId);
        return ResponseEntity.ok(GlobalResponse.success(transaction));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<GlobalResponse<List<Transaction>>> getTransactionsByStatus(
            @PathVariable TransactionStatus status) {
        List<Transaction> transactions = transactionService.getTransactionsByStatus(status);
        return ResponseEntity.ok(GlobalResponse.success(transactions));
    }

    @GetMapping("/{transactionId}/payments")
    public ResponseEntity<GlobalResponse<List<Payment>>> getPaymentsByTransactionId(@PathVariable UUID transactionId) {
        List<Payment> payments = transactionService.getPaymentsByTransactionId(transactionId);
        return ResponseEntity.ok(GlobalResponse.success(payments));
    }
}
