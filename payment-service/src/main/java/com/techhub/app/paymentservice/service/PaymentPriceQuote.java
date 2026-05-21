package com.techhub.app.paymentservice.service;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Value
@Builder
public class PaymentPriceQuote {
    UUID courseId;
    String courseTitle;
    BigDecimal originalAmount;
    String originalCurrency;
    BigDecimal gatewayAmount;
    String gatewayCurrency;
    BigDecimal fxRate;
    String fxProvider;
    OffsetDateTime fxQuotedAt;
}
