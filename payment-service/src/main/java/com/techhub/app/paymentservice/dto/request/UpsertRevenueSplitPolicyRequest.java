package com.techhub.app.paymentservice.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class UpsertRevenueSplitPolicyRequest {
    private String scope;
    private UUID instructorId;
    private UUID courseId;
    private BigDecimal instructorRate;
    private OffsetDateTime effectiveFrom;
    private OffsetDateTime effectiveTo;
}