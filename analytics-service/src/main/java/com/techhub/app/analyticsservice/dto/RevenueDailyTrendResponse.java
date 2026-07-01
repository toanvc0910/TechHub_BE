package com.techhub.app.analyticsservice.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;

@Value
@Builder
public class RevenueDailyTrendResponse {
    LocalDate metricDate;
    BigDecimal grossRevenue;
    BigDecimal instructorRevenue;
    BigDecimal adminRevenue;
    String policyScope;
    Integer policyVersion;
    Long totalOrders;
    Long totalItems;
}