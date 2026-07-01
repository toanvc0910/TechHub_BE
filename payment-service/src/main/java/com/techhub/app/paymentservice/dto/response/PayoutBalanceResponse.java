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
    // Quy đổi sang USD (làm tham khảo cho UI), tỉ giá tại thời điểm gọi.
    BigDecimal totalEarnedUsd;
    BigDecimal pendingAmountUsd;
    BigDecimal availableAmountUsd;
    BigDecimal usdRate;
    String currency;
}
