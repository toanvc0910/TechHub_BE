package com.techhub.app.paymentservice.repository.projection;

import java.math.BigDecimal;
import java.util.UUID;

public interface CoursePaymentPriceProjection {
    UUID getCourseId();

    String getTitle();

    BigDecimal getPrice();

    BigDecimal getDiscountPrice();

    String getCurrency();
}
