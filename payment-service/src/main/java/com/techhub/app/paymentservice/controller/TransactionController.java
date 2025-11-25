package com.techhub.app.paymentservice.controller;

import com.techhub.app.paymentservice.config.RestResponseObject;
import com.techhub.app.paymentservice.entity.Payment;
import com.techhub.app.paymentservice.entity.Transaction;
import com.techhub.app.paymentservice.entity.enums.TransactionStatus;
import com.techhub.app.paymentservice.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping("/user/{userId}")
    public RestResponseObject<List<Transaction>> getTransactionsByUserId(@PathVariable UUID userId) {
        List<Transaction> transactions = transactionService.getTransactionsByUserId(userId);
        return new RestResponseObject<>(HttpStatus.OK, "Success", transactions);
    }

    @GetMapping("/{transactionId}")
    public RestResponseObject<Transaction> getTransactionById(@PathVariable UUID transactionId) {
        Transaction transaction = transactionService.getTransactionById(transactionId);
        return new RestResponseObject<>(HttpStatus.OK, "Success", transaction);
    }

    @GetMapping("/status/{status}")
    public RestResponseObject<List<Transaction>> getTransactionsByStatus(@PathVariable TransactionStatus status) {
        List<Transaction> transactions = transactionService.getTransactionsByStatus(status);
        return new RestResponseObject<>(HttpStatus.OK, "Success", transactions);
    }

    @GetMapping("/{transactionId}/payments")
    public RestResponseObject<List<Payment>> getPaymentsByTransactionId(@PathVariable UUID transactionId) {
        List<Payment> payments = transactionService.getPaymentsByTransactionId(transactionId);
        return new RestResponseObject<>(HttpStatus.OK, "Success", payments);
    }
}

