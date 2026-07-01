package com.techhub.app.paymentservice.dto.response;

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
    BigDecimal estimatedInstructorRevenue;
    BigDecimal estimatedAdminRevenue;
    BigDecimal instructorRate;
    BigDecimal adminRate;
    Long totalOrders;
    Long totalItems;
    Long totalCourses;
}