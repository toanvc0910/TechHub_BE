package com.techhub.app.analyticsservice.repository;

import com.techhub.app.analyticsservice.entity.RevenueDailyAggregate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RevenueDailyAggregateRepository extends JpaRepository<RevenueDailyAggregate, UUID> {

        Optional<RevenueDailyAggregate> findByMetricDateAndInstructorId(LocalDate metricDate, UUID instructorId);

        @Query("SELECT COALESCE(SUM(r.grossRevenue), 0) FROM RevenueDailyAggregate r " +
                        "WHERE r.instructorId = :instructorId " +
                        "AND (:fromDate IS NULL OR r.metricDate >= :fromDate) " +
                        "AND (:toDate IS NULL OR r.metricDate <= :toDate)")
        BigDecimal sumInstructorGrossRevenue(@Param("instructorId") UUID instructorId,
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate);

        @Query("SELECT COALESCE(SUM(r.instructorRevenue), 0) FROM RevenueDailyAggregate r " +
                        "WHERE r.instructorId = :instructorId " +
                        "AND (:fromDate IS NULL OR r.metricDate >= :fromDate) " +
                        "AND (:toDate IS NULL OR r.metricDate <= :toDate)")
        BigDecimal sumInstructorNetRevenue(@Param("instructorId") UUID instructorId,
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate);

        @Query("SELECT COALESCE(SUM(r.grossRevenue), 0) FROM RevenueDailyAggregate r " +
                        "WHERE (:instructorId IS NULL OR r.instructorId = :instructorId) " +
                        "AND (:fromDate IS NULL OR r.metricDate >= :fromDate) " +
                        "AND (:toDate IS NULL OR r.metricDate <= :toDate)")
        BigDecimal sumAdminGrossRevenue(@Param("instructorId") UUID instructorId,
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate);

        @Query("SELECT COALESCE(SUM(r.adminRevenue), 0) FROM RevenueDailyAggregate r " +
                        "WHERE (:instructorId IS NULL OR r.instructorId = :instructorId) " +
                        "AND (:fromDate IS NULL OR r.metricDate >= :fromDate) " +
                        "AND (:toDate IS NULL OR r.metricDate <= :toDate)")
        BigDecimal sumAdminNetRevenue(@Param("instructorId") UUID instructorId,
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate);

        @Query("SELECT COALESCE(SUM(r.orderCount), 0) FROM RevenueDailyAggregate r " +
                        "WHERE (:instructorId IS NULL OR r.instructorId = :instructorId) " +
                        "AND (:fromDate IS NULL OR r.metricDate >= :fromDate) " +
                        "AND (:toDate IS NULL OR r.metricDate <= :toDate)")
        Long sumOrderCount(@Param("instructorId") UUID instructorId,
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate);

        @Query("SELECT COALESCE(SUM(r.itemCount), 0) FROM RevenueDailyAggregate r " +
                        "WHERE (:instructorId IS NULL OR r.instructorId = :instructorId) " +
                        "AND (:fromDate IS NULL OR r.metricDate >= :fromDate) " +
                        "AND (:toDate IS NULL OR r.metricDate <= :toDate)")
        Long sumItemCount(@Param("instructorId") UUID instructorId,
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate);

        List<RevenueDailyAggregate> findByInstructorIdAndMetricDateBetweenOrderByMetricDateAsc(UUID instructorId,
                        LocalDate fromDate, LocalDate toDate);

        List<RevenueDailyAggregate> findByMetricDateBetweenOrderByMetricDateAsc(LocalDate fromDate,
                        LocalDate toDate);
}
