package com.techhub.app.paymentservice.dto.response;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Value
@Builder
public class PayoutBatchResponse {
    UUID id;
    String batchName;
    String periodKey;
    OffsetDateTime fromDate;
    OffsetDateTime toDate;
    String status;
    Integer totalRequests;
    BigDecimal totalAmount;
    OffsetDateTime created;
}
