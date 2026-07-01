package com.techhub.app.paymentservice.dto.response;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.UUID;

@Value
@Builder
public class RevenueByCourseResponse {
    UUID courseId;
    String courseTitle;
    BigDecimal grossRevenue;
    Long soldCount;
    Long orderCount;
}