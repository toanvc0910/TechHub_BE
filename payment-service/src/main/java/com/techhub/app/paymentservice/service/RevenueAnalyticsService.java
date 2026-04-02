package com.techhub.app.paymentservice.service;

import com.techhub.app.paymentservice.dto.response.RevenueByCourseResponse;
import com.techhub.app.paymentservice.dto.response.RevenueOverviewResponse;
import com.techhub.app.paymentservice.repository.TransactionItemRepository;
import com.techhub.app.paymentservice.repository.projection.RevenueByCourseProjection;
import com.techhub.app.paymentservice.repository.projection.RevenueOverviewProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RevenueAnalyticsService {

    private final TransactionItemRepository transactionItemRepository;

    @Value("${payment.revenue.instructor-rate:0.7}")
    private BigDecimal instructorRate;

    @Transactional(readOnly = true)
    public RevenueOverviewResponse getInstructorOverview(UUID instructorId, LocalDate fromDate, LocalDate toDate) {
        OffsetDateTime from = fromDate == null ? null : fromDate.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime to = toDate == null ? null : toDate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        RevenueOverviewProjection projection = transactionItemRepository.getInstructorRevenueOverview(instructorId,
                from,
                to);
        return toOverviewResponse("INSTRUCTOR", instructorId, projection);
    }

    @Transactional(readOnly = true)
    public RevenueOverviewResponse getAdminOverview(UUID instructorId, LocalDate fromDate, LocalDate toDate) {
        OffsetDateTime from = fromDate == null ? null : fromDate.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime to = toDate == null ? null : toDate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        RevenueOverviewProjection projection = transactionItemRepository.getAdminRevenueOverview(instructorId, from,
                to);
        return toOverviewResponse("ADMIN", instructorId, projection);
    }

    @Transactional(readOnly = true)
    public List<RevenueByCourseResponse> getInstructorRevenueByCourse(UUID instructorId, LocalDate fromDate,
            LocalDate toDate) {
        OffsetDateTime from = fromDate == null ? null : fromDate.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime to = toDate == null ? null : toDate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        List<RevenueByCourseProjection> rows = transactionItemRepository.getInstructorRevenueByCourse(instructorId,
                from,
                to);
        return rows.stream()
                .map(it -> RevenueByCourseResponse.builder()
                        .courseId(it.getCourseId())
                        .courseTitle(it.getCourseTitle())
                        .grossRevenue(safeMoney(it.getGrossRevenue()))
                        .soldCount(safeLong(it.getSoldCount()))
                        .orderCount(safeLong(it.getOrderCount()))
                        .build())
                .collect(Collectors.toList());
    }

    private RevenueOverviewResponse toOverviewResponse(String scope, UUID instructorId, RevenueOverviewProjection row) {
        BigDecimal gross = safeMoney(row == null ? null : row.getGrossRevenue());
        BigDecimal normalizedInstructorRate = normalizeRate(instructorRate);
        BigDecimal adminRate = BigDecimal.ONE.subtract(normalizedInstructorRate);

        BigDecimal estimatedInstructorRevenue = gross.multiply(normalizedInstructorRate).setScale(2,
                RoundingMode.HALF_UP);
        BigDecimal estimatedAdminRevenue = gross.subtract(estimatedInstructorRevenue).setScale(2, RoundingMode.HALF_UP);

        return RevenueOverviewResponse.builder()
                .scope(scope)
                .instructorId(instructorId)
                .grossRevenue(gross)
                .estimatedInstructorRevenue(estimatedInstructorRevenue)
                .estimatedAdminRevenue(estimatedAdminRevenue)
                .instructorRate(normalizedInstructorRate)
                .adminRate(adminRate)
                .totalOrders(safeLong(row == null ? null : row.getTotalOrders()))
                .totalItems(safeLong(row == null ? null : row.getTotalItems()))
                .totalCourses(safeLong(row == null ? null : row.getTotalCourses()))
                .build();
    }

    private BigDecimal safeMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : value.setScale(2, RoundingMode.HALF_UP);
    }

    private Long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private BigDecimal normalizeRate(BigDecimal value) {
        if (value == null) {
            return BigDecimal.valueOf(0.7);
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        if (value.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ONE;
        }
        return value;
    }
}