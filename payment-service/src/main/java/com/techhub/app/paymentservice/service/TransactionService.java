package com.techhub.app.paymentservice.service;

import com.techhub.app.paymentservice.dto.response.PaymentHistoryItemResponse;
import com.techhub.app.paymentservice.entity.Payment;
import com.techhub.app.paymentservice.entity.Transaction;
import com.techhub.app.paymentservice.entity.enums.TransactionStatus;
import com.techhub.app.paymentservice.repository.PaymentRepository;
import com.techhub.app.paymentservice.repository.projection.PaymentHistoryProjection;
import com.techhub.app.paymentservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final PaymentRepository paymentRepository;
    private final RevenueSplitPolicyService revenueSplitPolicyService;

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
    public Page<PaymentHistoryItemResponse> getPaymentHistory(UUID requesterId, boolean adminView, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "created"));

        if (adminView) {
            return paymentRepository.findPaymentHistoryForAdmin(pageable)
                    .map(row -> toHistoryItem(row, null));
        }

        RevenueSplitPolicyService.ResolvedPolicy resolvedPolicy = revenueSplitPolicyService.resolvePolicy(
                requesterId,
                null,
                OffsetDateTime.now());
        BigDecimal instructorRate = normalizeRate(resolvedPolicy.getInstructorRate());
        return paymentRepository.findPaymentHistoryForInstructor(requesterId, pageable)
                .map(row -> toHistoryItem(row, instructorRate));
    }

    @Transactional(readOnly = true)
    public PaymentHistoryItemResponse getPaymentById(UUID paymentId) {
        return paymentRepository.findPaymentHistoryDetail(paymentId)
                .map(row -> {
                    // Resolve instructor rate dựa trên courseId nếu có; fallback default 0.7.
                    BigDecimal rate = BigDecimal.valueOf(0.7);
                    try {
                        if (row.getCourseId() != null) {
                            RevenueSplitPolicyService.ResolvedPolicy policy = revenueSplitPolicyService
                                    .resolvePolicy(null, row.getCourseId(), OffsetDateTime.now());
                            if (policy.getInstructorRate() != null) {
                                rate = policy.getInstructorRate();
                            }
                        }
                    } catch (Exception ignored) {
                    }
                    return toHistoryItem(row, rate);
                })
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
    }

    private PaymentHistoryItemResponse toHistoryItem(Payment payment) {
        Transaction transaction = payment.getTransaction();
        return PaymentHistoryItemResponse.builder()
                .id(payment.getId())
                .transactionId(transaction == null ? null : transaction.getId())
                .userId(transaction == null ? null : transaction.getUserId())
                .userName(null)
                .userEmail(null)
                .courseId(null)
                .courseName(null)
                .amount(transaction == null ? null : transaction.getAmount())
                .grossAmount(transaction == null ? null : transaction.getAmount())
                .instructorAmount(null)
                .adminAmount(null)
                .paymentMethod(payment.getMethod() == null ? null : payment.getMethod().name())
                .status(payment.getStatus() == null ? null : payment.getStatus().name())
                .currency(transaction == null || transaction.getOriginalCurrency() == null
                        ? "VND"
                        : transaction.getOriginalCurrency())
                .created(payment.getCreated() == null ? null : payment.getCreated().toOffsetDateTime())
                .updated(payment.getUpdated() == null ? null : payment.getUpdated().toOffsetDateTime())
                .build();
    }

    private PaymentHistoryItemResponse toHistoryItem(PaymentHistoryProjection row, BigDecimal instructorRate) {
        BigDecimal gross = safeMoney(row.getGrossAmount());
        BigDecimal normalizedRate = normalizeRate(instructorRate);
        BigDecimal instructorAmount = instructorRate == null ? null
                : gross.multiply(normalizedRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal adminAmount = instructorRate == null ? null
                : gross.subtract(instructorAmount).setScale(2, RoundingMode.HALF_UP);

        return PaymentHistoryItemResponse.builder()
                .id(row.getId())
                .transactionId(row.getTransactionId())
                .userId(row.getUserId())
                .userName(row.getUserName())
                .userEmail(row.getUserEmail())
                .courseId(row.getCourseId())
                .courseName(row.getCourseName())
                .amount(gross)
                .grossAmount(gross)
                .instructorAmount(instructorAmount)
                .adminAmount(adminAmount)
                .paymentMethod(row.getPaymentMethod())
                .status(row.getStatus())
                .currency(row.getCurrency() == null ? "VND" : row.getCurrency())
                .created(row.getCreated() == null ? null : row.getCreated().atOffset(java.time.ZoneOffset.UTC))
                .updated(row.getUpdated() == null ? null : row.getUpdated().atOffset(java.time.ZoneOffset.UTC))
                .build();
    }

    private BigDecimal safeMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeRate(BigDecimal value) {
        if (value == null) {
            return BigDecimal.valueOf(0.7);
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        if (value.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ONE;
        }
        return value;
    }
}
