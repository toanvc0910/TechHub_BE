package com.techhub.app.analyticsservice.service;

import com.techhub.app.analyticsservice.dto.RevenueOverviewResponse;
import com.techhub.app.analyticsservice.dto.RevenueDailyTrendResponse;
import com.techhub.app.analyticsservice.entity.RevenueDailyAggregate;
import com.techhub.app.analyticsservice.repository.RevenueDailyAggregateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RevenueProjectionService {

    private final RevenueDailyAggregateRepository revenueRepository;

    @Transactional
    public void applyRevenueSplit(UUID instructorId, BigDecimal gross, BigDecimal instructorAmount,
            BigDecimal adminAmount, int quantity, LocalDate metricDate, boolean firstItemForTransaction) {
        RevenueDailyAggregate row = revenueRepository.findByMetricDateAndInstructorId(metricDate, instructorId)
                .orElseGet(() -> RevenueDailyAggregate.builder()
                        .metricDate(metricDate)
                        .instructorId(instructorId)
                        .grossRevenue(BigDecimal.ZERO)
                        .instructorRevenue(BigDecimal.ZERO)
                        .adminRevenue(BigDecimal.ZERO)
                        .orderCount(0L)
                        .itemCount(0L)
                        .build());

        row.setGrossRevenue(row.getGrossRevenue().add(safe(gross)));
        row.setInstructorRevenue(row.getInstructorRevenue().add(safe(instructorAmount)));
        row.setAdminRevenue(row.getAdminRevenue().add(safe(adminAmount)));
        row.setItemCount(row.getItemCount() + Math.max(quantity, 1));
        if (firstItemForTransaction) {
            row.setOrderCount(row.getOrderCount() + 1);
        }

        revenueRepository.save(row);
    }

    @Transactional(readOnly = true)
    public RevenueOverviewResponse getInstructorOverview(UUID instructorId, LocalDate fromDate, LocalDate toDate) {
        return RevenueOverviewResponse.builder()
                .scope("INSTRUCTOR")
                .instructorId(instructorId)
                .grossRevenue(safe(revenueRepository.sumInstructorGrossRevenue(instructorId, fromDate, toDate)))
                .instructorRevenue(safe(revenueRepository.sumInstructorNetRevenue(instructorId, fromDate, toDate)))
                .adminRevenue(BigDecimal.ZERO)
                .totalOrders(safeLong(revenueRepository.sumOrderCount(instructorId, fromDate, toDate)))
                .totalItems(safeLong(revenueRepository.sumItemCount(instructorId, fromDate, toDate)))
                .build();
    }

    @Transactional(readOnly = true)
    public RevenueOverviewResponse getAdminOverview(UUID instructorId, LocalDate fromDate, LocalDate toDate) {
        return RevenueOverviewResponse.builder()
                .scope("ADMIN")
                .instructorId(instructorId)
                .grossRevenue(safe(revenueRepository.sumAdminGrossRevenue(instructorId, fromDate, toDate)))
                .instructorRevenue(BigDecimal.ZERO)
                .adminRevenue(safe(revenueRepository.sumAdminNetRevenue(instructorId, fromDate, toDate)))
                .totalOrders(safeLong(revenueRepository.sumOrderCount(instructorId, fromDate, toDate)))
                .totalItems(safeLong(revenueRepository.sumItemCount(instructorId, fromDate, toDate)))
                .build();
    }

    @Transactional(readOnly = true)
    public List<RevenueDailyTrendResponse> getInstructorTrend(UUID instructorId, LocalDate fromDate,
            LocalDate toDate) {
        return revenueRepository.findByInstructorIdAndMetricDateBetweenOrderByMetricDateAsc(instructorId, fromDate,
                toDate)
                .stream()
                .map(row -> RevenueDailyTrendResponse.builder()
                        .metricDate(row.getMetricDate())
                        .grossRevenue(safe(row.getGrossRevenue()))
                        .instructorRevenue(safe(row.getInstructorRevenue()))
                        .adminRevenue(safe(row.getAdminRevenue()))
                        .totalOrders(safeLong(row.getOrderCount()))
                        .totalItems(safeLong(row.getItemCount()))
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RevenueDailyTrendResponse> getAdminTrend(UUID instructorId, LocalDate fromDate, LocalDate toDate) {
        return getInstructorTrend(instructorId, fromDate, toDate);
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Long safeLong(Long value) {
        return value == null ? 0L : value;
    }
}
