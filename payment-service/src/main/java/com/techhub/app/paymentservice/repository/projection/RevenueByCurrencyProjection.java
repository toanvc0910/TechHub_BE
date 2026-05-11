package com.techhub.app.paymentservice.repository.projection;

import java.math.BigDecimal;

public interface RevenueByCurrencyProjection {
    String getCurrency();

    BigDecimal getGrossRevenue();
}
