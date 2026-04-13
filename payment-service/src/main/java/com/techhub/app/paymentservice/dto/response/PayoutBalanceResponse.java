package com.techhub.app.paymentservice.dto.response;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.UUID;

@Value
@Builder
public class PayoutBalanceResponse {
    UUID instructorId;
    BigDecimal totalEarned;
    BigDecimal pendingAmount;
    BigDecimal availableAmount;
}
