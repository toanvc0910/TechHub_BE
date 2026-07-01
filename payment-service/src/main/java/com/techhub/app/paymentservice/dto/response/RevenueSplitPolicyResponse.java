package com.techhub.app.paymentservice.dto.response;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Value
@Builder
public class RevenueSplitPolicyResponse {
    UUID id;
    String scope;
    UUID instructorId;
    UUID courseId;
    BigDecimal instructorRate;
    BigDecimal adminRate;
    Integer version;
    OffsetDateTime effectiveFrom;
    OffsetDateTime effectiveTo;
    String isActive;
}