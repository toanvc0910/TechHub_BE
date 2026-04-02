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
    OffsetDateTime computedAt;

    @Singular
    List<ItemSplit> items;

    @Value
    @Builder
    public static class ItemSplit {
        UUID courseId;
        UUID instructorId;
        BigDecimal grossAmount;
        BigDecimal instructorAmount;
        BigDecimal adminAmount;
        Integer quantity;
    }
}