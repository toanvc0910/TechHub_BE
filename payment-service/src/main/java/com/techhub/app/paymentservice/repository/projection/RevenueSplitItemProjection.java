package com.techhub.app.paymentservice.repository.projection;

import java.math.BigDecimal;
import java.util.UUID;

public interface RevenueSplitItemProjection {
    UUID getCourseId();

    UUID getInstructorId();

    BigDecimal getGrossAmount();

    Integer getQuantity();
}