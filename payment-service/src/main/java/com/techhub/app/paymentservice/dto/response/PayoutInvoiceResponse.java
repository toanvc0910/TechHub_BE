package com.techhub.app.paymentservice.dto.response;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Value
@Builder
public class PayoutInvoiceResponse {
    UUID id;
    String invoiceNumber;
    UUID payoutRequestId;
    UUID instructorId;
    BigDecimal amount;
    String transferReference;
    String status;
    Boolean emailSent;
    Boolean uiVisible;
    String pdfUrl;
    OffsetDateTime created;
    OffsetDateTime updated;
}