package com.techhub.app.paymentservice.dto.event;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Value
@Builder
public class RevenueSplitRecordedEvent {
    UUID transactionId;
    BigDecimal instructorRate;
    BigDecimal adminRate;
    Integer policyVersion;
    String policyScope;
    OffsetDateTime computedAt;

    @Singular
    List<ItemSplit> items;

    @Value
    @Builder
    public static class ItemSplit {
        UUID courseId;
        UUID instructorId;
        UUID policyId;
        String policyScope;
        Integer policyVersion;
        BigDecimal instructorRate;
        BigDecimal adminRate;
        BigDecimal grossAmount;
        BigDecimal instructorAmount;
        BigDecimal adminAmount;
        Integer quantity;
    }
}