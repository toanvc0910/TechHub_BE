package com.techhub.app.paymentservice.dto.response;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class PayoutOperationsSummaryResponse {
    long approvedRequests;
    BigDecimal totalRequested;
    long loadedRequests;
    long batchCount;
}
