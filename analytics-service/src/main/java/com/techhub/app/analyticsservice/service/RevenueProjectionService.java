package com.techhub.app.analyticsservice.service;

import com.techhub.app.analyticsservice.dto.RevenueOverviewResponse;
import com.techhub.app.analyticsservice.dto.RevenueDailyTrendResponse;
import com.techhub.app.analyticsservice.entity.RevenueDailyAggregate;
import com.techhub.app.analyticsservice.repository.RevenueDailyAggregateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class RevenueProjectionService {

        private static final LocalDate MIN_METRIC_DATE = LocalDate.of(1970, 1, 1);
        private static final LocalDate MAX_METRIC_DATE = LocalDate.of(2999, 12, 31);

        private final RevenueDailyAggregateRepository revenueRepository;

        @Transactional
        public void applyRevenueSplit(UUID instructorId, BigDecimal gross, BigDecimal instructorAmount,
                        BigDecimal adminAmount, String policyScope, Integer policyVersion,
                        int quantity, LocalDate metricDate, boolean firstItemForTransaction) {
                log.info("[RevenueProjection] applyRevenueSplit START instructorId={} metricDate={} gross={} instructor={} admin={} qty={} firstItem={}",
                                instructorId, metricDate, gross, instructorAmount, adminAmount, quantity, firstItemForTransaction);

                java.util.Optional<RevenueDailyAggregate> existing = revenueRepository
                                .findByMetricDateAndInstructorId(metricDate, instructorId);
                log.info("[RevenueProjection] existing row found={} for metricDate={} instructorId={}",
                                existing.isPresent(), metricDate, instructorId);

                RevenueDailyAggregate row = existing.orElseGet(() -> RevenueDailyAggregate.builder()
                                .metricDate(metricDate)
                                .instructorId(instructorId)
                                .grossRevenue(BigDecimal.ZERO)
                                .instructorRevenue(BigDecimal.ZERO)
                                .adminRevenue(BigDecimal.ZERO)
                                .orderCount(0L)
                                .itemCount(0L)
                                .build());

                BigDecimal beforeGross = row.getGrossRevenue();
                row.setGrossRevenue(row.getGrossRevenue().add(safe(gross)));
                row.setInstructorRevenue(row.getInstructorRevenue().add(safe(instructorAmount)));
                row.setAdminRevenue(row.getAdminRevenue().add(safe(adminAmount)));
                row.setPolicyScope(policyScope);
                row.setPolicyVersion(policyVersion);
                row.setItemCount(row.getItemCount() + Math.max(quantity, 1));
                if (firstItemForTransaction) {
                        row.setOrderCount(row.getOrderCount() + 1);
                }

                try {
                        RevenueDailyAggregate saved = revenueRepository.save(row);
                        log.info("[RevenueProjection] SAVED id={} instructorId={} metricDate={} grossBefore={} grossAfter={} instructorRevenue={} adminRevenue={} orderCount={} itemCount={}",
                                        saved.getId(), saved.getInstructorId(), saved.getMetricDate(),
                                        beforeGross, saved.getGrossRevenue(), saved.getInstructorRevenue(),
                                        saved.getAdminRevenue(), saved.getOrderCount(), saved.getItemCount());
                } catch (Exception ex) {
                        log.error("[RevenueProjection] SAVE FAILED instructorId={} metricDate={} error={}",
                                        instructorId, metricDate, ex.getMessage(), ex);
                        throw ex;
                }
        }

        @Transactional(readOnly = true)
        public RevenueOverviewResponse getInstructorOverview(UUID instructorId, LocalDate fromDate, LocalDate toDate) {
                LocalDate effectiveFromDate = normalizeFromDate(fromDate);
                LocalDate effectiveToDate = normalizeToDate(toDate);
                return RevenueOverviewResponse.builder()
                                .scope("INSTRUCTOR")
                                .instructorId(instructorId)
                                .grossRevenue(safe(
                                                revenueRepository.sumInstructorGrossRevenue(instructorId,
                                                                effectiveFromDate,
                                                                effectiveToDate)))
                                .instructorRevenue(safe(revenueRepository.sumInstructorNetRevenue(instructorId,
                                                effectiveFromDate, effectiveToDate)))
                                .adminRevenue(BigDecimal.ZERO)
                                .policyScope("MIXED")
                                .policyVersion(null)
                                .totalOrders(safeLong(
                                                revenueRepository.sumOrderCount(instructorId, effectiveFromDate,
                                                                effectiveToDate)))
                                .totalItems(safeLong(
                                                revenueRepository.sumItemCount(instructorId, effectiveFromDate,
                                                                effectiveToDate)))
                                .build();
        }

        @Transactional(readOnly = true)
        public RevenueOverviewResponse getAdminOverview(UUID instructorId, LocalDate fromDate, LocalDate toDate) {
                LocalDate effectiveFromDate = normalizeFromDate(fromDate);
                LocalDate effectiveToDate = normalizeToDate(toDate);
                return RevenueOverviewResponse.builder()
                                .scope("ADMIN")
                                .instructorId(instructorId)
                                .grossRevenue(safe(
                                                revenueRepository.sumAdminGrossRevenue(instructorId,
                                                                effectiveFromDate,
                                                                effectiveToDate)))
                                .instructorRevenue(BigDecimal.ZERO)
                                .adminRevenue(safe(
                                                revenueRepository.sumAdminNetRevenue(instructorId,
                                                                effectiveFromDate,
                                                                effectiveToDate)))
                                .policyScope("MIXED")
                                .policyVersion(null)
                                .totalOrders(safeLong(
                                                revenueRepository.sumOrderCount(instructorId, effectiveFromDate,
                                                                effectiveToDate)))
                                .totalItems(safeLong(
                                                revenueRepository.sumItemCount(instructorId, effectiveFromDate,
                                                                effectiveToDate)))
                                .build();
        }

        @Transactional(readOnly = true)
        public List<RevenueDailyTrendResponse> getInstructorTrend(UUID instructorId, LocalDate fromDate,
                        LocalDate toDate) {
                LocalDate effectiveFromDate = normalizeFromDate(fromDate);
                LocalDate effectiveToDate = normalizeToDate(toDate);
                return revenueRepository.findInstructorTrendRows(instructorId, effectiveFromDate,
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
