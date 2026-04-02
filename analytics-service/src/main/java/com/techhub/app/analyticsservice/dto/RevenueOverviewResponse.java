package com.techhub.app.analyticsservice.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.UUID;

@Value
@Builder
public class RevenueOverviewResponse {
    String scope;
    UUID instructorId;
    BigDecimal grossRevenue;
    BigDecimal instructorRevenue;
    BigDecimal adminRevenue;
    Long totalOrders;
    Long totalItems;
}
