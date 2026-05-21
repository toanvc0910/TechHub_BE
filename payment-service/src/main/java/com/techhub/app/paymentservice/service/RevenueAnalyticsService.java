package com.techhub.app.paymentservice.service;

import com.techhub.app.paymentservice.dto.response.RevenueByCourseResponse;
import com.techhub.app.paymentservice.dto.response.RevenueOverviewResponse;
import com.techhub.app.paymentservice.repository.TransactionItemRepository;
import com.techhub.app.paymentservice.repository.projection.RevenueByCurrencyProjection;
import com.techhub.app.paymentservice.repository.projection.RevenueByCourseProjection;
import com.techhub.app.paymentservice.repository.projection.RevenueOverviewProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RevenueAnalyticsService {

    private final TransactionItemRepository transactionItemRepository;
    private final RevenueSplitPolicyService revenueSplitPolicyService;
    private final CurrencyExchangeService currencyExchangeService;

    @Transactional(readOnly = true)
    public RevenueOverviewResponse getInstructorOverview(UUID instructorId, LocalDate fromDate, LocalDate toDate) {
        OffsetDateTime from = fromDate == null ? null : fromDate.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime to = toDate == null ? null : toDate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        RevenueOverviewProjection projection = transactionItemRepository.getInstructorRevenueOverview(instructorId,
                from,
                to);
        BigDecimal grossInVnd = revenueRowsToVnd(
                transactionItemRepository.getInstructorRevenueByCurrency(instructorId, from, to));
        return toOverviewResponse("INSTRUCTOR", instructorId, projection, grossInVnd);
    }

    @Transactional(readOnly = true)
    public RevenueOverviewResponse getAdminOverview(UUID instructorId, LocalDate fromDate, LocalDate toDate) {
        OffsetDateTime from = fromDate == null ? null : fromDate.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime to = toDate == null ? null : toDate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        RevenueOverviewProjection projection = transactionItemRepository.getAdminRevenueOverview(instructorId, from,
                to);
        BigDecimal grossInVnd = revenueRowsToVnd(
                transactionItemRepository.getAdminRevenueByCurrency(instructorId, from, to));
        return toOverviewResponse("ADMIN", instructorId, projection, grossInVnd);
    }

    @Transactional(readOnly = true)
    public List<RevenueByCourseResponse> getInstructorRevenueByCourse(UUID instructorId, LocalDate fromDate,
            LocalDate toDate) {
        OffsetDateTime from = fromDate == null ? null : fromDate.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime to = toDate == null ? null : toDate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        List<RevenueByCourseProjection> rows = transactionItemRepository.getInstructorRevenueByCourse(instructorId,
                from,
                to);
        Map<UUID, CourseRevenueAccumulator> merged = new LinkedHashMap<>();
        for (RevenueByCourseProjection row : rows) {
            CourseRevenueAccumulator acc = merged.computeIfAbsent(row.getCourseId(),
                    id -> new CourseRevenueAccumulator(row.getCourseId(), row.getCourseTitle()));
            acc.grossRevenue = acc.grossRevenue.add(convertToVnd(safeMoney(row.getGrossRevenue()), row.getCurrency()));
            acc.soldCount += safeLong(row.getSoldCount());
            acc.orderCount += safeLong(row.getOrderCount());
        }

        return merged.values().stream()
                .sorted(Comparator.comparing((CourseRevenueAccumulator it) -> it.grossRevenue).reversed())
                .map(it -> RevenueByCourseResponse.builder()
                        .courseId(it.courseId)
                        .courseTitle(it.courseTitle)
                        .grossRevenue(safeMoney(it.grossRevenue))
                        .soldCount(it.soldCount)
                        .orderCount(it.orderCount)
                        .build())
                .collect(Collectors.toList());
    }

    private RevenueOverviewResponse toOverviewResponse(String scope, UUID instructorId, RevenueOverviewProjection row,
            BigDecimal grossOverride) {
        BigDecimal gross = safeMoney(grossOverride);
        RevenueSplitPolicyService.ResolvedPolicy resolvedPolicy = revenueSplitPolicyService.resolvePolicy(
                scope.equals("INSTRUCTOR") ? instructorId : null,
                null,
                OffsetDateTime.now());
        BigDecimal normalizedInstructorRate = normalizeRate(resolvedPolicy.getInstructorRate());
        BigDecimal adminRate = BigDecimal.ONE.subtract(normalizedInstructorRate).setScale(4, RoundingMode.HALF_UP);

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

    private BigDecimal revenueRowsToVnd(List<RevenueByCurrencyProjection> rows) {
        BigDecimal total = BigDecimal.ZERO;
        for (RevenueByCurrencyProjection row : rows) {
            total = total.add(convertToVnd(safeMoney(row.getGrossRevenue()), row.getCurrency()));
        }
        return safeMoney(total);
    }

    private BigDecimal convertToVnd(BigDecimal amount, String currency) {
        String code = currency == null ? "VND" : currency.toUpperCase();
        if ("VND".equals(code)) {
            return safeMoney(amount);
        }
        return safeMoney(currencyExchangeService.convert(amount, code, "VND"));
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

    private static class CourseRevenueAccumulator {
        private final UUID courseId;
        private final String courseTitle;
        private BigDecimal grossRevenue = BigDecimal.ZERO;
        private long soldCount;
        private long orderCount;

        private CourseRevenueAccumulator(UUID courseId, String courseTitle) {
            this.courseId = courseId;
            this.courseTitle = courseTitle;
        }
    }
}
