package com.techhub.app.paymentservice.service;

import com.techhub.app.paymentservice.dto.response.PaymentHistoryItemResponse;
import com.techhub.app.paymentservice.entity.Payment;
import com.techhub.app.paymentservice.entity.Transaction;
import com.techhub.app.paymentservice.entity.enums.TransactionStatus;
import com.techhub.app.paymentservice.repository.PaymentRepository;
import com.techhub.app.paymentservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByUserId(UUID userId) {
        log.info("Getting transactions for user: {}", userId);
        return transactionRepository.findByUserIdAndIsActive(userId, "Y");
    }

    @Transactional(readOnly = true)
    public Transaction getTransactionById(UUID transactionId) {
        log.info("Getting transaction by ID: {}", transactionId);
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));
    }

    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByStatus(TransactionStatus status) {
        log.info("Getting transactions by status: {}", status);
        return transactionRepository.findByStatusAndIsActive(status, "Y");
    }

    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByTransactionId(UUID transactionId) {
        log.info("Getting payments for transaction: {}", transactionId);
        return paymentRepository.findByTransactionIdAndIsActive(transactionId, "Y");
    }

    @Transactional(readOnly = true)
    public Page<PaymentHistoryItemResponse> getPaymentHistory(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "created"));

        return paymentRepository.findByIsActive("Y", pageable)
                .map(this::toHistoryItem);
    }

    @Transactional(readOnly = true)
    public PaymentHistoryItemResponse getPaymentById(UUID paymentId) {
        Payment payment = paymentRepository.findByIdAndIsActive(paymentId, "Y")
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
        return toHistoryItem(payment);
    }

    private PaymentHistoryItemResponse toHistoryItem(Payment payment) {
        Transaction transaction = payment.getTransaction();
        return PaymentHistoryItemResponse.builder()
                .id(payment.getId())
                .transactionId(transaction == null ? null : transaction.getId())
                .userId(transaction == null ? null : transaction.getUserId())
                .amount(transaction == null ? null : transaction.getAmount())
                .paymentMethod(payment.getMethod() == null ? null : payment.getMethod().name())
                .status(payment.getStatus() == null ? null : payment.getStatus().name())
                .created(payment.getCreated())
                .updated(payment.getUpdated())
                .build();
    }
}
