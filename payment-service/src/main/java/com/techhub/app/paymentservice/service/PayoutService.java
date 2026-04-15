package com.techhub.app.paymentservice.service;

import com.techhub.app.paymentservice.dto.request.CreatePayoutRequestRequest;
import com.techhub.app.paymentservice.dto.request.MarkPaidPayoutRequest;
import com.techhub.app.paymentservice.dto.request.ReviewPayoutRequestRequest;
import com.techhub.app.paymentservice.dto.response.PayoutBalanceResponse;
import com.techhub.app.paymentservice.dto.response.PayoutBatchResponse;
import com.techhub.app.paymentservice.dto.response.PayoutRequestResponse;
import com.techhub.app.paymentservice.dto.response.RevenueOverviewResponse;
import com.techhub.app.paymentservice.entity.PayoutBatch;
import com.techhub.app.paymentservice.entity.PayoutLedgerEntry;
import com.techhub.app.paymentservice.entity.PayoutRequest;
import com.techhub.app.paymentservice.entity.enums.PayoutBatchStatus;
import com.techhub.app.paymentservice.entity.enums.PayoutLedgerEntryType;
import com.techhub.app.paymentservice.entity.enums.PayoutRequestStatus;
import com.techhub.app.paymentservice.repository.PayoutBatchRepository;
import com.techhub.app.paymentservice.repository.PayoutLedgerEntryRepository;
import com.techhub.app.paymentservice.repository.PayoutRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PayoutService {

    private static final DateTimeFormatter PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final String REVENUE_BOOTSTRAP_REFERENCE = "REVENUE_BOOTSTRAP";

    private final PayoutRequestRepository payoutRequestRepository;
    private final PayoutBatchRepository payoutBatchRepository;
    private final PayoutLedgerEntryRepository payoutLedgerEntryRepository;
    private final RevenueAnalyticsService revenueAnalyticsService;

    @Transactional
    public PayoutBalanceResponse getBalance(UUID instructorId) {
        ensureRevenueCreditBootstrapped(instructorId);
        String instructorIdText = instructorId.toString();

        BigDecimal totalCredits = safeMoney(payoutLedgerEntryRepository.sumAmountByInstructorAndTypes(
                instructorIdText,
                Arrays.asList(PayoutLedgerEntryType.CREDIT_SALE.name(), PayoutLedgerEntryType.ADJUSTMENT.name())));

        BigDecimal totalDebits = safeMoney(payoutLedgerEntryRepository.sumAmountByInstructorAndTypes(
                instructorIdText,
                Arrays.asList(PayoutLedgerEntryType.DEBIT_REFUND.name(), PayoutLedgerEntryType.DEBIT_PAYOUT.name())));

        BigDecimal totalEarned = totalCredits.subtract(totalDebits).setScale(2, RoundingMode.HALF_UP);
        BigDecimal pendingAmount = safeMoney(payoutRequestRepository.sumAmountByInstructorAndStatuses(
                instructorIdText,
                Arrays.asList(PayoutRequestStatus.REQUESTED.name(), PayoutRequestStatus.APPROVED.name())));
        BigDecimal available = totalEarned.subtract(pendingAmount).max(BigDecimal.ZERO).setScale(2,
                RoundingMode.HALF_UP);

        return PayoutBalanceResponse.builder()
                .instructorId(instructorId)
                .totalEarned(totalEarned)
                .pendingAmount(pendingAmount)
                .availableAmount(available)
                .build();
    }

    @Transactional
    public PayoutRequestResponse createRequest(UUID instructorId, CreatePayoutRequestRequest request) {
        PayoutBalanceResponse balance = getBalance(instructorId);
        BigDecimal amount = safeMoney(request.getAmount());

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }
        if (amount.compareTo(balance.getAvailableAmount()) > 0) {
            throw new IllegalArgumentException("Requested amount exceeds available balance");
        }

        PayoutRequest saved = payoutRequestRepository.save(PayoutRequest.builder()
            .instructorId(instructorId.toString())
                .amount(amount)
                .note(request.getNote())
                .status(PayoutRequestStatus.REQUESTED)
                .build());

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<PayoutRequestResponse> listRequests(UUID requesterId, boolean adminView) {
        List<PayoutRequest> rows = adminView
                ? payoutRequestRepository.findByIsActiveOrderByCreatedDesc("Y")
                : payoutRequestRepository.findByInstructorIdAndIsActiveOrderByCreatedDesc(requesterId.toString(), "Y");
        return rows.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PayoutRequestResponse getRequest(UUID requestId, UUID requesterId, boolean adminView) {
        PayoutRequest request = payoutRequestRepository.findActiveById(requestId.toString())
                .orElseThrow(() -> new IllegalArgumentException("Payout request not found"));

        if (!adminView && !request.getInstructorId().equals(requesterId.toString())) {
            throw new IllegalArgumentException("You do not have permission to view this payout request");
        }

        return toResponse(request);
    }

    @Transactional
    public PayoutRequestResponse approveRequest(UUID requestId, UUID approverId, ReviewPayoutRequestRequest request) {
        PayoutRequest payoutRequest = payoutRequestRepository.findActiveById(requestId.toString())
                .orElseThrow(() -> new IllegalArgumentException("Payout request not found"));

        if (payoutRequest.getStatus() != PayoutRequestStatus.REQUESTED) {
            throw new IllegalArgumentException("Only REQUESTED payout can be approved");
        }

        payoutRequest.setStatus(PayoutRequestStatus.APPROVED);
        payoutRequest.setApprovedBy(approverId.toString());
        payoutRequest.setApprovedAt(OffsetDateTime.now());
        payoutRequest.setReviewNote(request.getNote());

        return toResponse(payoutRequestRepository.save(payoutRequest));
    }

    @Transactional
    public PayoutRequestResponse rejectRequest(UUID requestId, UUID reviewerId, ReviewPayoutRequestRequest request) {
        PayoutRequest payoutRequest = payoutRequestRepository.findActiveById(requestId.toString())
                .orElseThrow(() -> new IllegalArgumentException("Payout request not found"));

        if (payoutRequest.getStatus() != PayoutRequestStatus.REQUESTED
                && payoutRequest.getStatus() != PayoutRequestStatus.APPROVED) {
            throw new IllegalArgumentException("Only REQUESTED/APPROVED payout can be rejected");
        }

        payoutRequest.setStatus(PayoutRequestStatus.REJECTED);
        payoutRequest.setApprovedBy(reviewerId.toString());
        payoutRequest.setReviewNote(request.getNote());
        payoutRequest.setApprovedAt(OffsetDateTime.now());

        return toResponse(payoutRequestRepository.save(payoutRequest));
    }

    @Transactional
    public PayoutRequestResponse markPaid(UUID requestId, UUID markerId, MarkPaidPayoutRequest request) {
        PayoutRequest payoutRequest = payoutRequestRepository.findActiveById(requestId.toString())
                .orElseThrow(() -> new IllegalArgumentException("Payout request not found"));

        if (payoutRequest.getStatus() != PayoutRequestStatus.APPROVED) {
            throw new IllegalArgumentException("Only APPROVED payout can be marked paid");
        }

        payoutRequest.setStatus(PayoutRequestStatus.MARKED_PAID);
        payoutRequest.setPaymentReference(request.getPaymentReference());
        payoutRequest.setMarkedPaidBy(markerId.toString());
        payoutRequest.setMarkedPaidAt(OffsetDateTime.now());
        payoutRequest.setReviewNote(request.getNote());

        payoutLedgerEntryRepository.save(PayoutLedgerEntry.builder()
                .instructorId(payoutRequest.getInstructorId())
                .entryType(PayoutLedgerEntryType.DEBIT_PAYOUT)
                .amount(safeMoney(payoutRequest.getAmount()))
                .referenceId(payoutRequest.getId())
                .referenceType("PAYOUT_REQUEST")
                .note("Sandbox mark paid: " + request.getPaymentReference())
                .build());

        return toResponse(payoutRequestRepository.save(payoutRequest));
    }

    @Transactional(readOnly = true)
    public List<PayoutBatchResponse> listBatches() {
        return payoutBatchRepository.findByIsActiveOrderByCreatedDesc("Y")
                .stream()
                .map(this::toBatchResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PayoutBatchResponse createManualBatch(String batchName, LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null || !toDate.isAfter(fromDate)) {
            throw new IllegalArgumentException("Invalid date range for manual batch");
        }

        String periodKey = fromDate.getYear() + "-MANUAL";
        PayoutBatch batch = PayoutBatch.builder()
                .batchName(batchName == null || batchName.isBlank() ? "Manual Batch" : batchName)
                .periodKey(periodKey)
                .fromDate(fromDate.atStartOfDay().atOffset(ZoneOffset.UTC))
                .toDate(toDate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC))
                .status(PayoutBatchStatus.DRAFT)
                .totalAmount(BigDecimal.ZERO)
                .totalRequests(0)
                .build();

        PayoutBatch saved = payoutBatchRepository.save(batch);
        return toBatchResponse(saved);
    }

    @Transactional
    public PayoutBatchResponse createMonthlyBatch(YearMonth yearMonth) {
        YearMonth period = yearMonth == null ? YearMonth.now().minusMonths(1) : yearMonth;
        String periodKey = period.format(PERIOD_FORMAT);
        String batchName = "MONTHLY_" + periodKey;

        payoutBatchRepository.findByPeriodKeyAndBatchNameAndIsActive(periodKey, batchName, "Y")
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Monthly batch already exists for " + periodKey);
                });

        LocalDate firstDay = period.atDay(1);
        LocalDate lastDay = period.atEndOfMonth();
        OffsetDateTime from = firstDay.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime to = lastDay.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        PayoutBatch batch = payoutBatchRepository.save(PayoutBatch.builder()
                .batchName(batchName)
                .periodKey(periodKey)
                .fromDate(from)
                .toDate(to)
                .status(PayoutBatchStatus.DRAFT)
                .totalAmount(BigDecimal.ZERO)
                .totalRequests(0)
                .build());

        return toBatchResponse(batch);
    }

    @Scheduled(cron = "0 10 1 1 * *")
    @Transactional
    public void autoCreateMonthlyBatch() {
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        try {
            createMonthlyBatch(lastMonth);
        } catch (IllegalArgumentException ignored) {
            // Batch already exists; safe to ignore in scheduled mode.
        }
    }

    private void ensureRevenueCreditBootstrapped(UUID instructorId) {
        String instructorIdText = instructorId.toString();
        boolean alreadyBootstrapped = payoutLedgerEntryRepository
                .existsByInstructorIdAndReferenceTypeAndIsActive(instructorIdText, REVENUE_BOOTSTRAP_REFERENCE, "Y");
        if (alreadyBootstrapped) {
            return;
        }

        RevenueOverviewResponse overview = revenueAnalyticsService.getInstructorOverview(instructorId, null, null);
        BigDecimal earned = safeMoney(overview.getEstimatedInstructorRevenue());
        if (earned.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        payoutLedgerEntryRepository.save(PayoutLedgerEntry.builder()
            .instructorId(instructorId.toString())
                .entryType(PayoutLedgerEntryType.CREDIT_SALE)
                .amount(earned)
                .referenceType(REVENUE_BOOTSTRAP_REFERENCE)
                .note("Bootstrap instructor earnings from revenue overview")
                .build());
    }

    private PayoutRequestResponse toResponse(PayoutRequest request) {
        return PayoutRequestResponse.builder()
            .id(parseUuidOrNull(request.getId()))
            .instructorId(parseUuidOrNull(request.getInstructorId()))
                .batchId(parseUuidOrNull(request.getBatchIdRaw()))
                .amount(request.getAmount())
                .status(request.getStatus().name())
                .note(request.getNote())
                .reviewNote(request.getReviewNote())
                .paymentReference(request.getPaymentReference())
                .approvedAt(request.getApprovedAt())
                .markedPaidAt(request.getMarkedPaidAt())
                .created(request.getCreated())
                .updated(request.getUpdated())
                .build();
    }

    private UUID parseUuidOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private PayoutBatchResponse toBatchResponse(PayoutBatch batch) {
        return PayoutBatchResponse.builder()
            .id(parseUuidOrNull(batch.getId()))
                .batchName(batch.getBatchName())
                .periodKey(batch.getPeriodKey())
                .fromDate(batch.getFromDate())
                .toDate(batch.getToDate())
                .status(batch.getStatus().name())
                .totalRequests(batch.getTotalRequests())
                .totalAmount(batch.getTotalAmount())
                .created(batch.getCreated())
                .build();
    }

    private BigDecimal safeMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : value.setScale(2, RoundingMode.HALF_UP);
    }
}
