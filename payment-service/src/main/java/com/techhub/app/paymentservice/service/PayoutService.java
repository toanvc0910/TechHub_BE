package com.techhub.app.paymentservice.service;

import com.techhub.app.paymentservice.dto.request.CreatePayoutRequestRequest;
import com.techhub.app.paymentservice.dto.request.MarkPaidPayoutRequest;
import com.techhub.app.paymentservice.dto.request.ReviewPayoutRequestRequest;
import com.techhub.app.paymentservice.dto.response.PayoutBalanceResponse;
import com.techhub.app.paymentservice.dto.response.PayoutBatchResponse;
import com.techhub.app.paymentservice.dto.response.PayoutInvoiceResponse;
import com.techhub.app.paymentservice.dto.response.PayoutRequestResponse;
import com.techhub.app.paymentservice.dto.response.RevenueOverviewResponse;
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
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PayoutService {

    private static final DateTimeFormatter PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter INVOICE_NUMBER_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String REVENUE_BOOTSTRAP_REFERENCE = "REVENUE_BOOTSTRAP";

    private final PayoutRequestRepository payoutRequestRepository;
    private final PayoutBatchRepository payoutBatchRepository;
    private final PayoutInvoiceRepository payoutInvoiceRepository;
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

        PayoutRequest approved = payoutRequestRepository.save(payoutRequest);
        PayoutInvoice invoice = createInvoiceForRequest(approved);

        // MVP auto-transfer sandbox: approved request is settled immediately.
        String transferReference = generateTransferReference(approved.getId());
        approved.setStatus(PayoutRequestStatus.MARKED_PAID);
        approved.setPaymentReference(transferReference);
        approved.setMarkedPaidBy(approverId.toString());
        approved.setMarkedPaidAt(OffsetDateTime.now());
        approved.setReviewNote(mergeReviewNote(request.getNote(), "AUTO_TRANSFERRED"));

        invoice.setTransferReference(transferReference);
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setEmailSent(Boolean.TRUE);
        invoice.setUiVisible(Boolean.TRUE);

        payoutLedgerEntryRepository.save(PayoutLedgerEntry.builder()
                .instructorId(approved.getInstructorId())
                .entryType(PayoutLedgerEntryType.DEBIT_PAYOUT)
                .amount(safeMoney(approved.getAmount()))
                .referenceId(approved.getId())
                .referenceType("PAYOUT_REQUEST")
                .note("Auto transfer on approval: " + transferReference)
                .build());

        payoutInvoiceRepository.save(invoice);
        PayoutRequest settled = payoutRequestRepository.save(approved);
        return toResponse(settled, invoice);
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
            Document document = new Document();
            PdfWriter.getInstance(document, outputStream);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 11);

            document.add(new Paragraph("TechHub Payout Invoice", titleFont));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100f);
            table.setWidths(new float[] { 3f, 5f });

            addPdfRow(table, "Invoice Number", invoice.getInvoiceNumber(), sectionFont, bodyFont);
            addPdfRow(table, "Invoice ID", invoice.getId(), sectionFont, bodyFont);
            addPdfRow(table, "Payout Request ID", invoice.getPayoutRequestId(), sectionFont, bodyFont);
            addPdfRow(table, "Instructor ID", invoice.getInstructorId(), sectionFont, bodyFont);
            addPdfRow(table, "Amount", safeMoney(invoice.getAmount()).toPlainString(), sectionFont, bodyFont);
            addPdfRow(table, "Transfer Reference", nullableText(invoice.getTransferReference()), sectionFont, bodyFont);
            addPdfRow(table, "Status", invoice.getStatus() == null ? "N/A" : invoice.getStatus().name(), sectionFont,
                    bodyFont);
            addPdfRow(table, "Created", nullableText(invoice.getCreated()), sectionFont, bodyFont);
            addPdfRow(table, "Updated", nullableText(invoice.getUpdated()), sectionFont, bodyFont);

            document.add(table);
            document.add(new Paragraph(" "));
            document.add(
                    new Paragraph("This document is generated automatically by TechHub payout service.", bodyFont));

            document.close();
            return outputStream.toByteArray();
        } catch (DocumentException ex) {
            throw new IllegalStateException("Unable to generate payout invoice pdf", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to generate payout invoice pdf", ex);
        }
    }

    private void addPdfRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        PdfPCell valueCell = new PdfPCell(new Phrase(nullableText(value), valueFont));
        labelCell.setPadding(6f);
        valueCell.setPadding(6f);
        table.addCell(labelCell);
        table.addCell(valueCell);
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
