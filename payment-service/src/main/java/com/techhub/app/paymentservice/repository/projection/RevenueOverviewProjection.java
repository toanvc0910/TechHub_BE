package com.techhub.app.paymentservice.repository.projection;

import java.math.BigDecimal;

public interface RevenueOverviewProjection {
    BigDecimal getGrossRevenue();

    Long getTotalOrders();

    Long getTotalItems();

    Long getTotalCourses();
}