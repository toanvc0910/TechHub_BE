package com.techhub.app.paymentservice.repository.projection;

import java.math.BigDecimal;

public interface RevenueSplitItemProjection {
    String getCourseId();

    String getInstructorId();

    BigDecimal getGrossAmount();

    Integer getQuantity();

    String getCurrency();
}