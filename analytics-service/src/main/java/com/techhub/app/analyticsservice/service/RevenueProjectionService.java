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
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RevenueProjectionService {

        private static final LocalDate MIN_METRIC_DATE = LocalDate.of(1970, 1, 1);
        private static final LocalDate MAX_METRIC_DATE = LocalDate.of(2999, 12, 31);

        private final RevenueDailyAggregateRepository revenueRepository;

        @Transactional
        public void applyRevenueSplit(UUID instructorId, BigDecimal gross, BigDecimal instructorAmount,
                        BigDecimal adminAmount, String policyScope, Integer policyVersion,
                        int quantity, LocalDate metricDate, boolean firstItemForTransaction) {
                String instructorIdText = stringifyUuid(instructorId);
                RevenueDailyAggregate row = revenueRepository
                                .findByMetricDateAndInstructorId(metricDate, instructorIdText)
                                .orElseGet(() -> RevenueDailyAggregate.builder()
                                                .metricDate(metricDate)
                                                .instructorId(instructorIdText)
                                                .grossRevenue(BigDecimal.ZERO)
                                                .instructorRevenue(BigDecimal.ZERO)
                                                .adminRevenue(BigDecimal.ZERO)
                                                .orderCount(0L)
                                                .itemCount(0L)
                                                .build());

                row.setGrossRevenue(row.getGrossRevenue().add(safe(gross)));
                row.setInstructorRevenue(row.getInstructorRevenue().add(safe(instructorAmount)));
                row.setAdminRevenue(row.getAdminRevenue().add(safe(adminAmount)));
                row.setPolicyScope(policyScope);
                row.setPolicyVersion(policyVersion);
                row.setItemCount(row.getItemCount() + Math.max(quantity, 1));
                if (firstItemForTransaction) {
                        row.setOrderCount(row.getOrderCount() + 1);
                }

                revenueRepository.save(row);
        }

        @Transactional(readOnly = true)
        public RevenueOverviewResponse getInstructorOverview(UUID instructorId, LocalDate fromDate, LocalDate toDate) {
                LocalDate effectiveFromDate = normalizeFromDate(fromDate);
                LocalDate effectiveToDate = normalizeToDate(toDate);
                String instructorIdText = stringifyUuid(instructorId);
                return RevenueOverviewResponse.builder()
                                .scope("INSTRUCTOR")
                                .instructorId(instructorId)
                                .grossRevenue(safe(
                                                revenueRepository.sumInstructorGrossRevenue(instructorIdText,
                                                                effectiveFromDate,
                                                                effectiveToDate)))
                                .instructorRevenue(safe(revenueRepository.sumInstructorNetRevenue(instructorIdText,
                                                effectiveFromDate, effectiveToDate)))
                                .adminRevenue(BigDecimal.ZERO)
                                .policyScope("MIXED")
                                .policyVersion(null)
                                .totalOrders(safeLong(
                                                revenueRepository.sumOrderCount(instructorIdText, effectiveFromDate,
                                                                effectiveToDate)))
                                .totalItems(safeLong(
                                                revenueRepository.sumItemCount(instructorIdText, effectiveFromDate,
                                                                effectiveToDate)))
                                .build();
        }

        @Transactional(readOnly = true)
        public RevenueOverviewResponse getAdminOverview(UUID instructorId, LocalDate fromDate, LocalDate toDate) {
                LocalDate effectiveFromDate = normalizeFromDate(fromDate);
                LocalDate effectiveToDate = normalizeToDate(toDate);
                String instructorIdText = stringifyNullableUuid(instructorId);
                return RevenueOverviewResponse.builder()
                                .scope("ADMIN")
                                .instructorId(instructorId)
                                .grossRevenue(safe(
                                                revenueRepository.sumAdminGrossRevenue(instructorIdText,
                                                                effectiveFromDate,
                                                                effectiveToDate)))
                                .instructorRevenue(BigDecimal.ZERO)
                                .adminRevenue(safe(
                                                revenueRepository.sumAdminNetRevenue(instructorIdText,
                                                                effectiveFromDate,
                                                                effectiveToDate)))
                                .policyScope("MIXED")
                                .policyVersion(null)
                                .totalOrders(safeLong(
                                                revenueRepository.sumOrderCount(instructorIdText, effectiveFromDate,
                                                                effectiveToDate)))
                                .totalItems(safeLong(
                                                revenueRepository.sumItemCount(instructorIdText, effectiveFromDate,
                                                                effectiveToDate)))
                                .build();
        }

        @Transactional(readOnly = true)
        public List<RevenueDailyTrendResponse> getInstructorTrend(UUID instructorId, LocalDate fromDate,
                        LocalDate toDate) {
                LocalDate effectiveFromDate = normalizeFromDate(fromDate);
                LocalDate effectiveToDate = normalizeToDate(toDate);
                return revenueRepository.findInstructorTrendRows(stringifyUuid(instructorId), effectiveFromDate,
                                effectiveToDate)
                                .stream()
                                .map(row -> RevenueDailyTrendResponse.builder()
                                                .metricDate(row.getMetricDate())
                                                .grossRevenue(safe(row.getGrossRevenue()))
                                                .instructorRevenue(safe(row.getInstructorRevenue()))
                                                .adminRevenue(safe(row.getAdminRevenue()))
                                                .policyScope(row.getPolicyScope())
                                                .policyVersion(row.getPolicyVersion())
                                                .totalOrders(safeLong(row.getOrderCount()))
                                                .totalItems(safeLong(row.getItemCount()))
                                                .build())
                                .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public List<RevenueDailyTrendResponse> getAdminTrend(UUID instructorId, LocalDate fromDate, LocalDate toDate) {
                LocalDate effectiveFromDate = normalizeFromDate(fromDate);
                LocalDate effectiveToDate = normalizeToDate(toDate);
                if (instructorId != null) {
                        return getInstructorTrend(instructorId, effectiveFromDate, effectiveToDate);
                }

                return revenueRepository.findAdminTrendRows(null, effectiveFromDate, effectiveToDate)
                                .stream()
                                .collect(Collectors.groupingBy(RevenueDailyAggregate::getMetricDate))
                                .entrySet().stream()
                                .sorted(Map.Entry.comparingByKey())
                                .map(entry -> {
                                        List<RevenueDailyAggregate> dailyRows = entry.getValue();
                                        return RevenueDailyTrendResponse.builder()
                                                        .metricDate(entry.getKey())
                                                        .grossRevenue(safe(dailyRows.stream()
                                                                        .map(RevenueDailyAggregate::getGrossRevenue)
                                                                        .reduce(BigDecimal.ZERO, BigDecimal::add)))
                                                        .instructorRevenue(safe(dailyRows.stream().map(
                                                                        RevenueDailyAggregate::getInstructorRevenue)
                                                                        .reduce(BigDecimal.ZERO, BigDecimal::add)))
                                                        .adminRevenue(safe(dailyRows.stream()
                                                                        .map(RevenueDailyAggregate::getAdminRevenue)
                                                                        .reduce(BigDecimal.ZERO, BigDecimal::add)))
                                                        .policyScope("MIXED")
                                                        .policyVersion(null)
                                                        .totalOrders(safeLong(dailyRows.stream()
                                                                        .mapToLong(row -> row.getOrderCount() == null
                                                                                        ? 0L
                                                                                        : row.getOrderCount())
                                                                        .sum()))
                                                        .totalItems(safeLong(dailyRows.stream()
                                                                        .mapToLong(row -> row.getItemCount() == null
                                                                                        ? 0L
                                                                                        : row.getItemCount())
                                                                        .sum()))
                                                        .build();
                                })
                                .collect(Collectors.toList());
        }

        private BigDecimal safe(BigDecimal value) {
                return value == null ? BigDecimal.ZERO : value;
        }

        private Long safeLong(Long value) {
                return value == null ? 0L : value;
        }

        private String stringifyUuid(UUID value) {
                return value.toString();
        }

        private String stringifyNullableUuid(UUID value) {
                return value == null ? null : value.toString();
        }

        private LocalDate normalizeFromDate(LocalDate fromDate) {
                return fromDate == null ? MIN_METRIC_DATE : fromDate;
        }

        private LocalDate normalizeToDate(LocalDate toDate) {
                return toDate == null ? MAX_METRIC_DATE : toDate;
        }
}
