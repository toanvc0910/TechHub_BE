package com.techhub.app.paymentservice.dto.response;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Value
@Builder
public class PayoutRequestResponse {
    UUID id;
    UUID instructorId;
    UUID batchId;
    BigDecimal amount;
    String status;
    String note;
    String reviewNote;
    String paymentReference;
    OffsetDateTime approvedAt;
    OffsetDateTime markedPaidAt;
    OffsetDateTime created;
    OffsetDateTime updated;
}
