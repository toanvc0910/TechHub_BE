package com.techhub.app.paymentservice.dto.event;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Value
@Builder
public class PaymentCompletedEvent {
    UUID transactionId;
    UUID userId;
    BigDecimal amount;
    String paymentMethod;
    OffsetDateTime completedAt;
}