package com.techhub.app.paymentservice.repository.projection;

import java.math.BigDecimal;
import java.util.UUID;

public interface RevenueByCourseProjection {
    UUID getCourseId();

    String getCourseTitle();

    BigDecimal getGrossRevenue();

    Long getSoldCount();

    Long getOrderCount();
}