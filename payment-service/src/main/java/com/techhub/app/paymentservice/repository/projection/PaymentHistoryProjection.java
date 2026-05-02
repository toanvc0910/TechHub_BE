package com.techhub.app.paymentservice.repository.projection;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface PaymentHistoryProjection {
    UUID getId();

    UUID getTransactionId();

    UUID getUserId();

    String getUserName();

    String getUserEmail();

    UUID getCourseId();

    String getCourseName();

    BigDecimal getGrossAmount();

    String getPaymentMethod();

    String getStatus();

    OffsetDateTime getCreated();

    OffsetDateTime getUpdated();
}