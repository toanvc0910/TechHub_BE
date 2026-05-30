package com.techhub.app.paymentservice.service;

import com.techhub.app.paymentservice.dto.request.CreatePayoutRequestRequest;
import com.techhub.app.paymentservice.dto.request.MarkPaidPayoutRequest;
import com.techhub.app.paymentservice.dto.request.ReviewPayoutRequestRequest;
import com.techhub.app.paymentservice.dto.response.PayoutBalanceResponse;
import com.techhub.app.paymentservice.dto.response.PayoutBatchResponse;
import com.techhub.app.paymentservice.dto.response.PayoutInvoiceResponse;
import com.techhub.app.paymentservice.dto.response.PayoutOperationsSummaryResponse;
import com.techhub.app.paymentservice.dto.response.PayoutRequestResponse;
import com.techhub.app.paymentservice.entity.PayoutBatch;
import com.techhub.app.paymentservice.entity.PayoutInvoice;
import com.techhub.app.paymentservice.entity.PayoutLedgerEntry;
import com.techhub.app.paymentservice.entity.PayoutRequest;
import com.techhub.app.paymentservice.entity.enums.InvoiceStatus;
import com.techhub.app.paymentservice.entity.enums.PayoutBatchStatus;
import com.techhub.app.paymentservice.entity.enums.PayoutLedgerEntryType;
import com.techhub.app.paymentservice.entity.enums.PayoutRequestStatus;
import com.techhub.app.paymentservice.repository.PayoutBatchRepository;
import com.techhub.app.paymentservice.repository.PayoutInvoiceRepository;
import com.techhub.app.paymentservice.repository.PayoutLedgerEntryRepository;
import com.techhub.app.paymentservice.repository.PayoutRequestRepository;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayoutService {

    private static final DateTimeFormatter PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter INVOICE_NUMBER_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter PDF_DATE_FORMAT = DateTimeFormatter
            .ofPattern("dd MMM yyyy, HH:mm:ss XXX")
            .withLocale(Locale.ENGLISH);
    private static final String REVENUE_BOOTSTRAP_REFERENCE = "REVENUE_BOOTSTRAP";
    private static final String TECHHUB_LEGAL_NAME = "TECHHUB LEARNING PLATFORM";
    private static final String TECHHUB_CONTACT = "support@techhub.com | techhub.com";
    private static final Color BRAND_NAVY = new Color(8, 47, 73);
    private static final Color BRAND_BLUE = new Color(41, 151, 229);
    private static final Color SOFT_BLUE = new Color(239, 248, 255);
    private static final Color SOFT_GRAY = new Color(246, 248, 251);
    private static final Color TEXT_MUTED = new Color(93, 112, 130);
    private static final Color PAID_GREEN = new Color(17, 145, 95);
    private static final Color STAMP_RED = new Color(203, 52, 70);

    private final PayoutRequestRepository payoutRequestRepository;
    private final PayoutBatchRepository payoutBatchRepository;
    private final PayoutInvoiceRepository payoutInvoiceRepository;
    private final PayoutLedgerEntryRepository payoutLedgerEntryRepository;
    private final com.techhub.app.paymentservice.repository.TransactionItemRepository transactionItemRepository;
    private final CurrencyExchangeService currencyExchangeService;
    private final RevenueSplitPolicyService revenueSplitPolicyService;
    private final RestTemplate restTemplate;

    @Value("${user-service.name:USER-SERVICE}")
    private String userServiceName;

    /** Tổng doanh thu của hệ thống (admin share) cộng dồn theo VND, không phụ thuộc ledger. */
    @Transactional(readOnly = true)
    public PayoutBalanceResponse getAdminBalance(UUID adminUserId) {
        BigDecimal grossInVnd = BigDecimal.ZERO;
        for (com.techhub.app.paymentservice.repository.projection.RevenueByCurrencyProjection row :
                transactionItemRepository.getAllRevenueByCurrency()) {
            BigDecimal gross = safeMoney(row.getGrossRevenue());
            if (gross.compareTo(BigDecimal.ZERO) <= 0) continue;
            String currency = row.getCurrency() == null ? "VND" : row.getCurrency().toUpperCase();
            BigDecimal grossVnd = "VND".equals(currency)
                    ? gross
                    : currencyExchangeService.convert(gross, currency, "VND");
            grossInVnd = grossInVnd.add(grossVnd);
        }
        // Lấy adminRate từ policy GLOBAL hiện tại; fallback 0.3.
        BigDecimal adminRate = BigDecimal.valueOf(0.3);
        try {
            RevenueSplitPolicyService.ResolvedPolicy policy = revenueSplitPolicyService
                    .resolvePolicy(null, null, OffsetDateTime.now());
            if (policy.getInstructorRate() != null) {
                adminRate = BigDecimal.ONE.subtract(policy.getInstructorRate());
            }
        } catch (Exception ignored) {
        }
        BigDecimal totalEarned = grossInVnd.multiply(adminRate).setScale(2, RoundingMode.HALF_UP);

        BigDecimal usdRate = BigDecimal.ZERO;
        BigDecimal totalEarnedUsd = BigDecimal.ZERO;
        try {
            usdRate = currencyExchangeService.getRate("VND", "USD");
            totalEarnedUsd = totalEarned.multiply(usdRate).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception ignored) {
        }

        return PayoutBalanceResponse.builder()
                .instructorId(adminUserId)
                .totalEarned(totalEarned)
                .pendingAmount(BigDecimal.ZERO)
                .availableAmount(totalEarned)
                .totalEarnedUsd(totalEarnedUsd)
                .pendingAmountUsd(BigDecimal.ZERO)
                .availableAmountUsd(totalEarnedUsd)
                .usdRate(usdRate)
                .currency("VND")
                .build();
    }

    @Transactional
    public PayoutBalanceResponse getBalance(UUID instructorId) {
        syncRevenueCredit(instructorId);
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

        BigDecimal usdRate = BigDecimal.ZERO;
        BigDecimal totalEarnedUsd = BigDecimal.ZERO;
        BigDecimal pendingAmountUsd = BigDecimal.ZERO;
        BigDecimal availableAmountUsd = BigDecimal.ZERO;
        try {
            usdRate = currencyExchangeService.getRate("VND", "USD");
            totalEarnedUsd = totalEarned.multiply(usdRate).setScale(2, RoundingMode.HALF_UP);
            pendingAmountUsd = pendingAmount.multiply(usdRate).setScale(2, RoundingMode.HALF_UP);
            availableAmountUsd = available.multiply(usdRate).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception ignored) {
            // Nếu FX fail thì để 0, không chặn balance.
        }

        return PayoutBalanceResponse.builder()
                .instructorId(instructorId)
                .totalEarned(totalEarned)
                .pendingAmount(pendingAmount)
                .availableAmount(available)
                .totalEarnedUsd(totalEarnedUsd)
                .pendingAmountUsd(pendingAmountUsd)
                .availableAmountUsd(availableAmountUsd)
                .usdRate(usdRate)
                .currency("VND")
                .build();
    }

    @Transactional
    public PayoutRequestResponse createRequest(UUID instructorId, CreatePayoutRequestRequest request) {
        PayoutBalanceResponse balance = getBalance(instructorId);
        BigDecimal amount = safeMoney(request.getAmount());

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }

        // Nếu instructor request bằng USD, quy đổi sang VND (canonical) để trừ ledger.
        String reqCurrency = request.getCurrency() == null ? "VND" : request.getCurrency().toUpperCase();
        BigDecimal amountVnd = "USD".equals(reqCurrency)
                ? currencyExchangeService.convert(amount, "USD", "VND")
                : amount;

        if (amountVnd.compareTo(balance.getAvailableAmount()) > 0) {
            throw new IllegalArgumentException("Requested amount exceeds available balance");
        }

        String note = request.getNote();
        if ("USD".equals(reqCurrency)) {
            String fxNote = "Requested " + amount.toPlainString() + " USD ≈ " + amountVnd.toPlainString() + " VND";
            note = note == null || note.isBlank() ? fxNote : note + " | " + fxNote;
        }

        PayoutRequest saved = payoutRequestRepository.save(PayoutRequest.builder()
                .instructorId(instructorId.toString())
                .amount(amountVnd)
                .note(note)
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
    public PayoutOperationsSummaryResponse getOperationsSummary(UUID requesterId, boolean adminView) {
        List<PayoutRequest> requests = adminView
                ? payoutRequestRepository.findByIsActiveOrderByCreatedDesc("Y")
                : payoutRequestRepository.findByInstructorIdAndIsActiveOrderByCreatedDesc(requesterId.toString(), "Y");
        BigDecimal totalRequested = requests.stream()
                .map(PayoutRequest::getAmount)
                .map(this::safeMoney)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        long approvedRequests = requests.stream()
                .filter(request -> request.getApprovedAt() != null)
                .count();
        long batchCount = adminView ? payoutBatchRepository.findByIsActiveOrderByCreatedDesc("Y").size() : 0;

        return PayoutOperationsSummaryResponse.builder()
                .approvedRequests(approvedRequests)
                .totalRequested(totalRequested)
                .loadedRequests(requests.size())
                .batchCount(batchCount)
                .build();
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

        PayoutRequest approved = payoutRequestRepository.save(payoutRequest);
        PayoutInvoice invoice = createInvoiceForRequest(approved);
        return toResponse(approved, invoice);
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
    public PayoutRequestResponse settleApprovedRequest(UUID requestId, UUID reviewerId,
            ReviewPayoutRequestRequest request) {
        PayoutRequest payoutRequest = payoutRequestRepository.findActiveById(requestId.toString())
                .orElseThrow(() -> new IllegalArgumentException("Payout request not found"));

        if (payoutRequest.getStatus() != PayoutRequestStatus.APPROVED) {
            throw new IllegalArgumentException("Only APPROVED payout can be settled");
        }

        PayoutInvoice invoice = createInvoiceForRequest(payoutRequest);
        String transferReference = generateTransferReference(payoutRequest.getId());

        payoutRequest.setStatus(PayoutRequestStatus.MARKED_PAID);
        payoutRequest.setPaymentReference(transferReference);
        payoutRequest.setMarkedPaidBy(reviewerId.toString());
        payoutRequest.setMarkedPaidAt(OffsetDateTime.now());
        payoutRequest.setReviewNote(mergeReviewNote(request.getNote(), "AUTO_TRANSFERRED"));

        invoice.setTransferReference(transferReference);
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setEmailSent(Boolean.TRUE);
        invoice.setUiVisible(Boolean.TRUE);

        payoutLedgerEntryRepository.save(PayoutLedgerEntry.builder()
                .instructorId(payoutRequest.getInstructorId())
                .entryType(PayoutLedgerEntryType.DEBIT_PAYOUT)
                .amount(safeMoney(payoutRequest.getAmount()))
                .referenceId(payoutRequest.getId())
                .referenceType("PAYOUT_REQUEST")
                .note("Legacy settlement on approved payout: " + transferReference)
                .build());

        payoutInvoiceRepository.save(invoice);
        return toResponse(payoutRequestRepository.save(payoutRequest), invoice);
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

        payoutInvoiceRepository.findByPayoutRequestIdAndIsActive(payoutRequest.getId(), "Y")
                .ifPresent(invoice -> {
                    invoice.setTransferReference(request.getPaymentReference());
                    invoice.setStatus(InvoiceStatus.PAID);
                    invoice.setEmailSent(Boolean.TRUE);
                    invoice.setUiVisible(Boolean.TRUE);
                    payoutInvoiceRepository.save(invoice);
                });

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
    public List<PayoutInvoiceResponse> listInvoices(UUID requesterId, boolean adminView,
            UUID instructorIdFilter) {
        if (adminView) {
            if (instructorIdFilter != null) {
                return payoutInvoiceRepository.findByInstructorIdAndIsActiveOrderByCreatedDesc(
                        instructorIdFilter.toString(), "Y")
                        .stream()
                        .map(this::toInvoiceResponse)
                        .collect(Collectors.toList());
            }
            return payoutInvoiceRepository.findByIsActiveOrderByCreatedDesc("Y")
                    .stream()
                    .map(this::toInvoiceResponse)
                    .collect(Collectors.toList());
        }

        return payoutInvoiceRepository.findByInstructorIdAndIsActiveOrderByCreatedDesc(
                requesterId.toString(), "Y")
                .stream()
                .map(this::toInvoiceResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PayoutInvoiceResponse getInvoice(UUID invoiceId, UUID requesterId, boolean adminView) {
        PayoutInvoice invoice = getAccessibleInvoice(invoiceId, requesterId, adminView);
        return toInvoiceResponse(invoice);
    }

    @Transactional(readOnly = true)
    public byte[] getInvoicePdf(UUID invoiceId, UUID requesterId, boolean adminView) {
        PayoutInvoice invoice = getAccessibleInvoice(invoiceId, requesterId, adminView);
        return buildInvoicePdf(invoice);
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

        String periodKey = fromDate.format(PERIOD_FORMAT);
        PayoutBatch batch = PayoutBatch.builder()
                .batchName(batchName == null || batchName.isBlank() ? "Manual Batch" : batchName)
                .periodKey(periodKey)
                .fromDate(fromDate.atStartOfDay().atOffset(ZoneOffset.UTC))
                .toDate(toDate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC))
                .status(PayoutBatchStatus.DRAFT)
                .totalAmount(BigDecimal.ZERO)
                .totalRequests(0)
                .build();

        PayoutBatch saved = populateBatch(payoutBatchRepository.save(batch));
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

        PayoutBatch batch = populateBatch(payoutBatchRepository.save(PayoutBatch.builder()
                .batchName(batchName)
                .periodKey(periodKey)
                .fromDate(from)
                .toDate(to)
                .status(PayoutBatchStatus.DRAFT)
                .totalAmount(BigDecimal.ZERO)
                .totalRequests(0)
                .build()));

        return toBatchResponse(batch);
    }

    private PayoutBatch populateBatch(PayoutBatch batch) {
        List<PayoutRequest> requests = payoutRequestRepository.findUnbatchedRequestedInRange(
                batch.getFromDate(), batch.getToDate());
        requests.forEach(request -> request.setBatch(batch));
        payoutRequestRepository.saveAll(requests);

        BigDecimal totalAmount = requests.stream()
                .map(PayoutRequest::getAmount)
                .map(this::safeMoney)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        batch.setTotalRequests(requests.size());
        batch.setTotalAmount(totalAmount);
        return payoutBatchRepository.save(batch);
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

    private void syncRevenueCredit(UUID instructorId) {
        String instructorIdText = instructorId.toString();
        log.info("[PayoutSync] START instructorId={}", instructorIdText);

        // Tổng gross theo từng currency rồi quy đổi về VND (canonical cho payout).
        java.util.List<com.techhub.app.paymentservice.repository.projection.RevenueByCurrencyProjection> rows =
                transactionItemRepository.getInstructorRevenueByCurrency(instructorId, null, null);
        log.info("[PayoutSync] revenue rows count={} instructorId={}", rows.size(), instructorIdText);

        BigDecimal grossInVnd = BigDecimal.ZERO;
        for (com.techhub.app.paymentservice.repository.projection.RevenueByCurrencyProjection row : rows) {
            BigDecimal gross = safeMoney(row.getGrossRevenue());
            String currency = row.getCurrency() == null ? "VND" : row.getCurrency().toUpperCase();
            log.info("[PayoutSync] revenue row currency={} gross={}", currency, gross);
            if (gross.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal grossVnd = "VND".equals(currency)
                    ? gross
                    : currencyExchangeService.convert(gross, currency, "VND");
            log.info("[PayoutSync] convert {} {} -> {} VND", gross, currency, grossVnd);
            grossInVnd = grossInVnd.add(grossVnd);
        }
        grossInVnd = grossInVnd.setScale(2, RoundingMode.HALF_UP);
        log.info("[PayoutSync] grossInVnd total={} instructorId={}", grossInVnd, instructorIdText);

        // Áp dụng policy chia doanh thu cho giảng viên.
        BigDecimal instructorRate = revenueSplitPolicyService
                .resolvePolicy(instructorId, null, OffsetDateTime.now())
                .getInstructorRate();
        if (instructorRate == null) {
            instructorRate = BigDecimal.valueOf(0.7);
        }
        BigDecimal earned = grossInVnd.multiply(instructorRate).setScale(2, RoundingMode.HALF_UP);
        log.info("[PayoutSync] instructorRate={} earned={} instructorId={}", instructorRate, earned, instructorIdText);
        if (earned.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("[PayoutSync] earned<=0, skip insert ledger. instructorId={}", instructorIdText);
            return;
        }

        BigDecimal alreadySynced = safeMoney(payoutLedgerEntryRepository
                .sumAmountByInstructorAndReferenceType(instructorIdText, REVENUE_BOOTSTRAP_REFERENCE));
        BigDecimal delta = earned.subtract(alreadySynced).setScale(2, RoundingMode.HALF_UP);
        log.info("[PayoutSync] alreadySynced={} delta={} instructorId={}", alreadySynced, delta, instructorIdText);
        if (delta.compareTo(BigDecimal.ZERO) == 0) {
            log.info("[PayoutSync] delta=0, no ledger adjustment needed. instructorId={}", instructorIdText);
            return;
        }

        PayoutLedgerEntryType entryType = delta.compareTo(BigDecimal.ZERO) > 0
                ? PayoutLedgerEntryType.CREDIT_SALE
                : PayoutLedgerEntryType.ADJUSTMENT;
        PayoutLedgerEntry inserted = payoutLedgerEntryRepository.save(PayoutLedgerEntry.builder()
                .instructorId(instructorIdText)
                .entryType(entryType)
                .amount(delta)
                .referenceType(REVENUE_BOOTSTRAP_REFERENCE)
                .note("Sync instructor earnings from revenue overview (currency-normalized delta)")
                .build());
        log.info("[PayoutSync] INSERTED ledger entry id={} type={} amount={} instructorId={}",
                inserted.getId(), inserted.getEntryType(), inserted.getAmount(), instructorIdText);
    }

    private PayoutRequestResponse toResponse(PayoutRequest request) {
        PayoutInvoice invoice = payoutInvoiceRepository
                .findByPayoutRequestIdAndIsActive(request.getId(), "Y")
                .orElse(null);
        return toResponse(request, invoice);
    }

    private PayoutRequestResponse toResponse(PayoutRequest request, PayoutInvoice invoice) {
        return PayoutRequestResponse.builder()
                .id(parseUuidOrNull(request.getId()))
                .instructorId(parseUuidOrNull(request.getInstructorId()))
                .batchId(parseUuidOrNull(request.getBatchIdRaw()))
                .invoiceId(invoice == null ? null : parseUuidOrNull(invoice.getId()))
                .invoiceNumber(invoice == null ? null : invoice.getInvoiceNumber())
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

    private PayoutInvoice createInvoiceForRequest(PayoutRequest request) {
        return payoutInvoiceRepository.findByPayoutRequestIdAndIsActive(request.getId(), "Y")
                .orElseGet(() -> payoutInvoiceRepository.save(PayoutInvoice.builder()
                        .invoiceNumber(generateInvoiceNumber(request.getId()))
                        .payoutRequestId(request.getId())
                        .instructorId(request.getInstructorId())
                        .amount(safeMoney(request.getAmount()))
                        .status(InvoiceStatus.GENERATED)
                        .emailSent(Boolean.FALSE)
                        .uiVisible(Boolean.TRUE)
                        .build()));
    }

    private PayoutInvoice getAccessibleInvoice(UUID invoiceId, UUID requesterId, boolean adminView) {
        PayoutInvoice invoice = payoutInvoiceRepository.findById(invoiceId.toString())
                .orElseThrow(() -> new IllegalArgumentException("Payout invoice not found"));

        if (!"Y".equals(invoice.getIsActive())) {
            throw new IllegalArgumentException("Payout invoice not found");
        }

        if (!adminView && !invoice.getInstructorId().equals(requesterId.toString())) {
            throw new IllegalArgumentException("You do not have permission to view this payout invoice");
        }

        return invoice;
    }

    private byte[] buildInvoicePdf(PayoutInvoice invoice) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 34f, 34f, 24f, 32f);
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            document.open();

            PayoutRequest payoutRequest = payoutRequestRepository.findActiveById(invoice.getPayoutRequestId())
                    .orElse(null);
            addInvoiceHeader(document, invoice);
            addInvoiceParties(document, invoice);
            addAmountSummary(document, invoice);
            addInvoiceDetails(document, invoice, payoutRequest);
            addAuditTrail(document, payoutRequest);
            addInvoiceNotes(document, payoutRequest);
            drawPaidSeal(writer, invoice);
            drawInvoiceFooter(writer, invoice);

            document.close();
            return outputStream.toByteArray();
        } catch (DocumentException ex) {
            throw new IllegalStateException("Unable to generate payout invoice pdf", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to generate payout invoice pdf", ex);
        }
    }

    private void addInvoiceHeader(Document document, PayoutInvoice invoice) throws Exception {
        PdfPTable header = new PdfPTable(new float[] { 1.1f, 3.6f, 3.4f });
        header.setWidthPercentage(100f);

        PdfPCell logoCell = borderlessCell();
        Image logo = loadTechHubLogo();
        if (logo != null) {
            logo.scaleToFit(58f, 58f);
            logoCell.addElement(logo);
        }
        header.addCell(logoCell);

        PdfPCell brandCell = borderlessCell();
        brandCell.addElement(paragraph("TECHHUB", 18f, Font.BOLD, BRAND_NAVY, 0f));
        brandCell.addElement(paragraph("LEARNING PLATFORM", 8f, Font.BOLD, BRAND_BLUE, 2f));
        brandCell.addElement(paragraph(TECHHUB_CONTACT, 8f, Font.NORMAL, TEXT_MUTED, 5f));
        header.addCell(brandCell);

        PdfPCell titleCell = borderlessCell();
        titleCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph title = paragraph("PAYOUT INVOICE", 17f, Font.BOLD, BRAND_NAVY, 0f);
        title.setAlignment(Element.ALIGN_RIGHT);
        titleCell.addElement(title);
        Paragraph number = paragraph(nullableText(invoice.getInvoiceNumber()), 9f, Font.BOLD, BRAND_BLUE, 5f);
        number.setAlignment(Element.ALIGN_RIGHT);
        titleCell.addElement(number);
        Paragraph issued = paragraph("Issued: " + formatPdfDate(invoice.getCreated()), 8f, Font.NORMAL, TEXT_MUTED, 4f);
        issued.setAlignment(Element.ALIGN_RIGHT);
        titleCell.addElement(issued);
        header.addCell(titleCell);
        document.add(header);

        PdfPTable rule = new PdfPTable(1);
        rule.setWidthPercentage(100f);
        PdfPCell ruleCell = new PdfPCell();
        ruleCell.setFixedHeight(3f);
        ruleCell.setBorder(Rectangle.NO_BORDER);
        ruleCell.setBackgroundColor(BRAND_BLUE);
        rule.addCell(ruleCell);
        rule.setSpacingBefore(7f);
        rule.setSpacingAfter(9f);
        document.add(rule);
    }

    private void addInvoiceParties(Document document, PayoutInvoice invoice) throws DocumentException {
        PdfPTable parties = new PdfPTable(new float[] { 1f, 1f });
        parties.setWidthPercentage(100f);
        parties.setSpacingAfter(8f);

        parties.addCell(partyCard("PAID BY", TECHHUB_LEGAL_NAME,
                "Platform payout operations",
                TECHHUB_CONTACT,
                "Settlement currency: VND"));

        // Enrich the recipient block with the instructor's real name and avatar.
        // Best-effort: if user-service is unavailable we fall back to the
        // generic account label so the invoice still renders.
        RecipientInfo recipient = fetchRecipientInfo(invoice.getInstructorId());
        String recipientName = (recipient != null && recipient.name != null && !recipient.name.isBlank())
                ? recipient.name
                : "TECHHUB INSTRUCTOR ACCOUNT";
        Image avatar = recipient == null ? null : loadAvatarImage(recipient.avatarUrl);
        parties.addCell(recipientCard("PAID TO", recipientName, avatar,
                recipient != null && recipient.email != null && !recipient.email.isBlank()
                        ? recipient.email
                        : "Instructor ID: " + nullableText(invoice.getInstructorId()),
                "Recipient profile verified by TechHub",
                "Account settlement beneficiary"));
        document.add(parties);
    }

    /** Lightweight holder for the recipient info shown on the invoice. */
    private static final class RecipientInfo {
        private final String name;
        private final String email;
        private final String avatarUrl;

        private RecipientInfo(String name, String email, String avatarUrl) {
            this.name = name;
            this.email = email;
            this.avatarUrl = avatarUrl;
        }
    }

    /**
     * Resolve the instructor's display name, email and avatar from user-service.
     * Returns null on any failure so PDF generation never breaks on this lookup.
     */
    @SuppressWarnings("unchecked")
    private RecipientInfo fetchRecipientInfo(String instructorId) {
        if (instructorId == null || instructorId.isBlank()) {
            return null;
        }
        String name = null;
        String email = null;
        String avatarUrl = null;

        // 1) Core user record: avatar + email + username.
        try {
            Map<String, Object> body = (Map<String, Object>) restTemplate.getForObject(
                    "http://" + userServiceName + "/api/users/" + instructorId, Map.class);
            Map<String, Object> data = body == null ? null : (Map<String, Object>) body.get("data");
            if (data != null) {
                Object avatarObj = data.get("avatar");
                if (avatarObj != null) avatarUrl = String.valueOf(avatarObj);
                Object emailObj = data.get("email");
                if (emailObj != null) email = String.valueOf(emailObj);
                Object usernameObj = data.get("username");
                if (usernameObj != null) name = String.valueOf(usernameObj);
            }
        } catch (Exception ex) {
            log.warn("Unable to load user {} for invoice recipient", instructorId, ex);
        }

        // 2) Instructor profile: prefer the real full name when available.
        try {
            Map<String, Object> body = (Map<String, Object>) restTemplate.getForObject(
                    "http://" + userServiceName + "/api/v1/instructor-profiles/" + instructorId, Map.class);
            Map<String, Object> data = body == null ? null : (Map<String, Object>) body.get("data");
            if (data != null) {
                Object fullName = data.get("fullName");
                if (fullName != null && !String.valueOf(fullName).isBlank()) {
                    name = String.valueOf(fullName);
                }
            }
        } catch (Exception ex) {
            log.debug("No instructor profile for {} (using user record name)", instructorId);
        }

        return new RecipientInfo(name, email, avatarUrl);
    }

    /** Download and decode the avatar image; returns null if unavailable. */
    private Image loadAvatarImage(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) {
            return null;
        }
        try {
            byte[] bytes = restTemplate.getForObject(avatarUrl, byte[].class);
            if (bytes == null || bytes.length == 0) {
                return null;
            }
            return Image.getInstance(bytes);
        } catch (Exception ex) {
            log.warn("Unable to load recipient avatar from {}", avatarUrl, ex);
            return null;
        }
    }

    /** Party card variant that renders the recipient avatar next to the name. */
    private PdfPCell recipientCard(String label, String name, Image avatar, String... lines)
            throws DocumentException {
        PdfPCell cell = styledCell(SOFT_GRAY, new Color(219, 226, 232), 8f);
        cell.addElement(paragraph(label, 8f, Font.BOLD, BRAND_BLUE, 0f));

        if (avatar != null) {
            // Name + avatar side by side in a borderless 2-column sub-table.
            PdfPTable head = new PdfPTable(new float[] { 1f, 5f });
            head.setWidthPercentage(100f);
            avatar.scaleToFit(26f, 26f);
            PdfPCell avatarCell = borderlessCell();
            avatarCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            avatarCell.addElement(avatar);
            head.addCell(avatarCell);
            PdfPCell nameCell = borderlessCell();
            nameCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            nameCell.addElement(paragraph(name, 11f, Font.BOLD, BRAND_NAVY, 0f));
            head.addCell(nameCell);
            head.setSpacingBefore(5f);
            cell.addElement(head);
        } else {
            cell.addElement(paragraph(name, 11f, Font.BOLD, BRAND_NAVY, 5f));
        }

        for (String line : lines) {
            cell.addElement(paragraph(line, 8f, Font.NORMAL, TEXT_MUTED, 3f));
        }
        return cell;
    }

    private void addAmountSummary(Document document, PayoutInvoice invoice) throws DocumentException {
        PdfPTable summary = new PdfPTable(new float[] { 3.6f, 2f });
        summary.setWidthPercentage(100f);
        summary.setSpacingAfter(9f);

        PdfPCell amountCell = styledCell(SOFT_BLUE, BRAND_BLUE, 9f);
        amountCell.addElement(paragraph("TOTAL PAYOUT", 9f, Font.BOLD, TEXT_MUTED, 0f));
        amountCell.addElement(paragraph(formatVnd(invoice.getAmount()), 25f, Font.BOLD, BRAND_NAVY, 5f));
        amountCell.addElement(paragraph("Net settlement amount transferred to the instructor account.",
                8f, Font.NORMAL, TEXT_MUTED, 6f));
        summary.addCell(amountCell);

        PdfPCell statusCell = styledCell(SOFT_GRAY, new Color(219, 226, 232), 9f);
        statusCell.addElement(paragraph("PAYMENT STATUS", 9f, Font.BOLD, TEXT_MUTED, 0f));
        statusCell.addElement(paragraph(invoice.getStatus() == null ? "N/A" : invoice.getStatus().name(),
                17f, Font.BOLD, PAID_GREEN, 7f));
        statusCell.addElement(paragraph("Transfer ref: " + nullableText(invoice.getTransferReference()),
                8f, Font.NORMAL, TEXT_MUTED, 7f));
        summary.addCell(statusCell);
        document.add(summary);
    }

    private void addInvoiceDetails(Document document, PayoutInvoice invoice, PayoutRequest payoutRequest)
            throws DocumentException {
        addSectionTitle(document, "SETTLEMENT DETAILS");
        PdfPTable details = new PdfPTable(new float[] { 2.2f, 4.8f });
        details.setWidthPercentage(100f);
        addPdfRow(details, "Invoice number", invoice.getInvoiceNumber());
        addPdfRow(details, "Invoice ID", invoice.getId());
        addPdfRow(details, "Payout request ID", invoice.getPayoutRequestId());
        addPdfRow(details, "Instructor ID", invoice.getInstructorId());
        addPdfRow(details, "Batch ID", payoutRequest == null ? null : payoutRequest.getBatchIdRaw());
        addPdfRow(details, "Transfer reference", invoice.getTransferReference());
        addPdfRow(details, "Payment channel", "TechHub platform payout settlement");
        addPdfRow(details, "Currency", "VND - Vietnamese Dong");
        addPdfRow(details, "Created at", formatPdfDate(invoice.getCreated()));
        addPdfRow(details, "Last updated", formatPdfDate(invoice.getUpdated()));
        document.add(details);
    }

    private void addAuditTrail(Document document, PayoutRequest payoutRequest) throws DocumentException {
        addSectionTitle(document, "PAYMENT AUDIT TRAIL");
        PdfPTable audit = new PdfPTable(new float[] { 2.2f, 4.8f });
        audit.setWidthPercentage(100f);
        addPdfRow(audit, "Requested at", payoutRequest == null ? null : formatPdfDate(payoutRequest.getCreated()));
        addPdfRow(audit, "Approved by", payoutRequest == null ? null : payoutRequest.getApprovedBy());
        addPdfRow(audit, "Approved at", payoutRequest == null ? null : formatPdfDate(payoutRequest.getApprovedAt()));
        addPdfRow(audit, "Marked paid by", payoutRequest == null ? null : payoutRequest.getMarkedPaidBy());
        addPdfRow(audit, "Marked paid at",
                payoutRequest == null ? null : formatPdfDate(payoutRequest.getMarkedPaidAt()));
        addPdfRow(audit, "Verification key", buildVerificationKey(payoutRequest));
        document.add(audit);
    }

    private void addInvoiceNotes(Document document, PayoutRequest payoutRequest) throws DocumentException {
        addSectionTitle(document, "NOTES & DECLARATION");
        PdfPTable notes = new PdfPTable(1);
        notes.setWidthPercentage(100f);
        PdfPCell notesCell = styledCell(SOFT_GRAY, new Color(219, 226, 232), 7f);
        notesCell.addElement(paragraph("Request note: " + nullableText(payoutRequest == null ? null : payoutRequest.getNote()),
                8f, Font.NORMAL, TEXT_MUTED, 0f));
        notesCell.addElement(paragraph(
                "Review note: " + nullableText(payoutRequest == null ? null : payoutRequest.getReviewNote()),
                8f, Font.NORMAL, TEXT_MUTED, 5f));
        notesCell.addElement(paragraph(
                "This invoice confirms a TechHub platform payout. Recipient identity is reconciled against the instructor account before settlement. This document is generated electronically and is valid without a handwritten signature.",
                8f, Font.NORMAL, TEXT_MUTED, 8f));
        notes.addCell(notesCell);
        document.add(notes);
    }

    private void addSectionTitle(Document document, String title) throws DocumentException {
        Paragraph section = paragraph(title, 9f, Font.BOLD, BRAND_NAVY, 5f);
        section.setSpacingAfter(3f);
        document.add(section);
    }

    private PdfPCell partyCard(String label, String title, String... lines) {
        PdfPCell cell = styledCell(SOFT_GRAY, new Color(219, 226, 232), 8f);
        cell.addElement(paragraph(label, 8f, Font.BOLD, BRAND_BLUE, 0f));
        cell.addElement(paragraph(title, 11f, Font.BOLD, BRAND_NAVY, 5f));
        for (String line : lines) {
            cell.addElement(paragraph(line, 8f, Font.NORMAL, TEXT_MUTED, 3f));
        }
        return cell;
    }

    private void addPdfRow(PdfPTable table, String label, Object value) {
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f, BRAND_NAVY);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 8f, TEXT_MUTED);
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        PdfPCell valueCell = new PdfPCell(new Phrase(nullableText(value), valueFont));
        labelCell.setPadding(3.5f);
        valueCell.setPadding(3.5f);
        labelCell.setBackgroundColor(SOFT_GRAY);
        labelCell.setBorderColor(new Color(219, 226, 232));
        valueCell.setBorderColor(new Color(219, 226, 232));
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private PdfPCell borderlessCell() {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(0f);
        return cell;
    }

    private PdfPCell styledCell(Color background, Color border, float padding) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(background);
        cell.setBorderColor(border);
        cell.setPadding(padding);
        return cell;
    }

    private Paragraph paragraph(String text, float size, int style,
            Color color, float spacingBefore) {
        Paragraph paragraph = new Paragraph(nullableText(text), FontFactory.getFont(FontFactory.HELVETICA, size, style, color));
        paragraph.setSpacingBefore(spacingBefore);
        return paragraph;
    }

    private Image loadTechHubLogo() {
        try {
            ClassPathResource logo = new ClassPathResource("branding/techhub-logo.png");
            return Image.getInstance(logo.getInputStream().readAllBytes());
        } catch (Exception ex) {
            log.warn("Unable to load TechHub invoice logo", ex);
            return null;
        }
    }

    private void drawPaidSeal(PdfWriter writer, PayoutInvoice invoice) throws Exception {
        if (invoice.getStatus() != InvoiceStatus.PAID) {
            return;
        }
        PdfContentByte canvas = writer.getDirectContent();
        PdfGState state = new PdfGState();
        state.setFillOpacity(0.22f);
        state.setStrokeOpacity(0.44f);
        canvas.saveState();
        canvas.setGState(state);
        canvas.setColorStroke(STAMP_RED);
        canvas.setColorFill(STAMP_RED);
        canvas.setLineWidth(2f);
        canvas.circle(474f, 118f, 49f);
        canvas.stroke();
        canvas.circle(474f, 118f, 42f);
        canvas.stroke();
        canvas.beginText();
        canvas.setFontAndSize(BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, false), 15f);
        canvas.showTextAligned(Element.ALIGN_CENTER, "TECHHUB", 474f, 131f, -8f);
        canvas.setFontAndSize(BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, false), 18f);
        canvas.showTextAligned(Element.ALIGN_CENTER, "PAID", 474f, 108f, -8f);
        canvas.setFontAndSize(BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, false), 8f);
        canvas.showTextAligned(Element.ALIGN_CENTER, "VERIFIED SETTLEMENT", 474f, 91f, -8f);
        canvas.endText();
        canvas.restoreState();
    }

    private void drawInvoiceFooter(PdfWriter writer, PayoutInvoice invoice) throws Exception {
        PdfContentByte canvas = writer.getDirectContent();
        canvas.saveState();
        canvas.setColorStroke(new Color(219, 226, 232));
        canvas.moveTo(38f, 28f);
        canvas.lineTo(557f, 28f);
        canvas.stroke();
        canvas.beginText();
        canvas.setColorFill(TEXT_MUTED);
        canvas.setFontAndSize(BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, false), 7f);
        canvas.showTextAligned(Element.ALIGN_LEFT, "TechHub payout service | Electronically generated document", 38f, 17f, 0f);
        canvas.showTextAligned(Element.ALIGN_RIGHT, nullableText(invoice.getInvoiceNumber()), 557f, 17f, 0f);
        canvas.endText();
        canvas.restoreState();
    }

    private String formatVnd(BigDecimal amount) {
        NumberFormat formatter = NumberFormat.getIntegerInstance(Locale.US);
        return formatter.format(safeMoney(amount).setScale(0, RoundingMode.HALF_UP)) + " VND";
    }

    private String formatPdfDate(OffsetDateTime value) {
        return value == null ? "N/A" : value.format(PDF_DATE_FORMAT);
    }

    private String buildVerificationKey(PayoutRequest payoutRequest) {
        String source = payoutRequest == null ? null : payoutRequest.getPaymentReference();
        if (source == null || source.isBlank()) {
            source = payoutRequest == null ? null : payoutRequest.getId();
        }
        return source == null ? "N/A" : "TECHHUB-" + source.replace("-", "").toUpperCase(Locale.ROOT);
    }

    private String nullableText(Object value) {
        return value == null ? "N/A" : String.valueOf(value);
    }

    private PayoutInvoiceResponse toInvoiceResponse(PayoutInvoice invoice) {
        return PayoutInvoiceResponse.builder()
                .id(parseUuidOrNull(invoice.getId()))
                .invoiceNumber(invoice.getInvoiceNumber())
                .payoutRequestId(parseUuidOrNull(invoice.getPayoutRequestId()))
                .instructorId(parseUuidOrNull(invoice.getInstructorId()))
                .amount(invoice.getAmount())
                .transferReference(invoice.getTransferReference())
                .status(invoice.getStatus() == null ? null : invoice.getStatus().name())
                .emailSent(invoice.getEmailSent())
                .uiVisible(invoice.getUiVisible())
                .pdfUrl(invoice.getPdfUrl())
                .created(invoice.getCreated())
                .updated(invoice.getUpdated())
                .build();
    }

    private String generateInvoiceNumber(String payoutRequestId) {
        String shortRequest = payoutRequestId == null ? "NA"
                : payoutRequestId.replace("-", "").substring(0, Math.min(8, payoutRequestId.length()));
        return "INV-" + OffsetDateTime.now().format(INVOICE_NUMBER_DATE_FORMAT) + "-" + shortRequest;
    }

    private String generateTransferReference(String payoutRequestId) {
        String shortRequest = payoutRequestId == null ? "NA"
                : payoutRequestId.replace("-", "").substring(0, Math.min(10, payoutRequestId.length()));
        return "TRF-" + OffsetDateTime.now().format(INVOICE_NUMBER_DATE_FORMAT) + "-" + shortRequest;
    }

    private String mergeReviewNote(String userNote, String systemTag) {
        if (userNote == null || userNote.isBlank()) {
            return systemTag;
        }
        return userNote + " | " + systemTag;
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
