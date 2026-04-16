package com.techhub.app.paymentservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
public class PaymentHistoryItemResponse {
    private UUID id;
    private UUID transactionId;
    private UUID userId;
    private String userName;
    private String userEmail;
    private UUID courseId;
    private String courseName;
    private BigDecimal amount;
    private BigDecimal grossAmount;
    private BigDecimal instructorAmount;
    private BigDecimal adminAmount;
    private String paymentMethod;
    private String status;
    private ZonedDateTime created;
    private ZonedDateTime updated;
}
