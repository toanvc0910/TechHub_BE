package com.techhub.app.paymentservice.service;

import com.lowagie.text.pdf.PdfReader;
import com.techhub.app.paymentservice.entity.PayoutInvoice;
import com.techhub.app.paymentservice.entity.PayoutRequest;
import com.techhub.app.paymentservice.entity.enums.InvoiceStatus;
import com.techhub.app.paymentservice.repository.PayoutBatchRepository;
import com.techhub.app.paymentservice.repository.PayoutInvoiceRepository;
import com.techhub.app.paymentservice.repository.PayoutLedgerEntryRepository;
import com.techhub.app.paymentservice.repository.PayoutRequestRepository;
import com.techhub.app.paymentservice.repository.TransactionItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PayoutServiceInvoicePdfTests {

    @Test
    void shouldRenderBrandedPaidInvoicePdf() throws Exception {
        PayoutRequestRepository payoutRequestRepository = mock(PayoutRequestRepository.class);
        PayoutService payoutService = new PayoutService(
                payoutRequestRepository,
                mock(PayoutBatchRepository.class),
                mock(PayoutInvoiceRepository.class),
                mock(PayoutLedgerEntryRepository.class),
                mock(TransactionItemRepository.class),
                mock(CurrencyExchangeService.class),
                mock(RevenueSplitPolicyService.class));

        PayoutRequest request = PayoutRequest.builder()
                .id("dba671f5-c854-43f3-97c1-2f019aae923a")
                .instructorId("9a54a992-5fe9-4a6b-af7e-4a9c92d68fe5")
                .amount(new BigDecimal("3057455.00"))
                .paymentReference("TRF-20260502-dba671f5c8")
                .approvedBy("a7242d1e-6e17-4f90-b6dc-06ad40eb1a19")
                .approvedAt(OffsetDateTime.parse("2026-05-02T17:20:00+07:00"))
                .markedPaidBy("a7242d1e-6e17-4f90-b6dc-06ad40eb1a19")
                .markedPaidAt(OffsetDateTime.parse("2026-05-02T17:23:28+07:00"))
                .created(OffsetDateTime.parse("2026-05-02T16:55:00+07:00"))
                .note("Monthly instructor revenue settlement")
                .reviewNote("Verified and paid by TechHub operations")
                .build();
        when(payoutRequestRepository.findActiveById(request.getId())).thenReturn(Optional.of(request));

        PayoutInvoice invoice = PayoutInvoice.builder()
                .id("752cd1ca-8549-4665-a2de-b0444b6fdca3")
                .invoiceNumber("INV-20260502-dba671f5")
                .payoutRequestId(request.getId())
                .instructorId(request.getInstructorId())
                .amount(request.getAmount())
                .transferReference(request.getPaymentReference())
                .status(InvoiceStatus.PAID)
                .created(OffsetDateTime.parse("2026-05-02T17:23:28+07:00"))
                .updated(OffsetDateTime.parse("2026-05-02T17:23:28+07:00"))
                .build();

        byte[] pdf = (byte[]) ReflectionTestUtils.invokeMethod(payoutService, "buildInvoicePdf", invoice);

        assertThat(pdf).isNotNull();
        assertThat(pdf.length).isGreaterThan(10_000);
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");

        PdfReader reader = new PdfReader(pdf);
        assertThat(reader.getNumberOfPages()).isEqualTo(1);
        reader.close();

        Path preview = Path.of("target", "payout-invoice-preview.pdf");
        Files.createDirectories(preview.getParent());
        Files.write(preview, pdf);
    }
}
