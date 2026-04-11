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

        Optional<RevenueDailyAggregate> findByMetricDateAndInstructorId(LocalDate metricDate, String instructorId);

        @Query("SELECT COALESCE(SUM(r.grossRevenue), 0) FROM RevenueDailyAggregate r " +
                        "WHERE r.instructorId = :instructorId " +
                        "AND r.metricDate >= :fromDate " +
                        "AND r.metricDate <= :toDate")
        BigDecimal sumInstructorGrossRevenue(@Param("instructorId") String instructorId,
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate);

        @Query("SELECT COALESCE(SUM(r.instructorRevenue), 0) FROM RevenueDailyAggregate r " +
                        "WHERE r.instructorId = :instructorId " +
                        "AND r.metricDate >= :fromDate " +
                        "AND r.metricDate <= :toDate")
        BigDecimal sumInstructorNetRevenue(@Param("instructorId") String instructorId,
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate);

        @Query("SELECT COALESCE(SUM(r.grossRevenue), 0) FROM RevenueDailyAggregate r " +
                        "WHERE (:instructorId IS NULL OR r.instructorId = :instructorId) " +
                        "AND r.metricDate >= :fromDate " +
                        "AND r.metricDate <= :toDate")
        BigDecimal sumAdminGrossRevenue(@Param("instructorId") String instructorId,
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate);

        @Query("SELECT COALESCE(SUM(r.adminRevenue), 0) FROM RevenueDailyAggregate r " +
                        "WHERE (:instructorId IS NULL OR r.instructorId = :instructorId) " +
                        "AND r.metricDate >= :fromDate " +
                        "AND r.metricDate <= :toDate")
        BigDecimal sumAdminNetRevenue(@Param("instructorId") String instructorId,
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate);

        @Query("SELECT COALESCE(SUM(r.orderCount), 0) FROM RevenueDailyAggregate r " +
                        "WHERE (:instructorId IS NULL OR r.instructorId = :instructorId) " +
                        "AND r.metricDate >= :fromDate " +
                        "AND r.metricDate <= :toDate")
        Long sumOrderCount(@Param("instructorId") String instructorId,
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate);

        @Query("SELECT COALESCE(SUM(r.itemCount), 0) FROM RevenueDailyAggregate r " +
                        "WHERE (:instructorId IS NULL OR r.instructorId = :instructorId) " +
                        "AND r.metricDate >= :fromDate " +
                        "AND r.metricDate <= :toDate")
        Long sumItemCount(@Param("instructorId") String instructorId,
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate);

        @Query("SELECT r FROM RevenueDailyAggregate r " +
                        "WHERE r.instructorId = :instructorId " +
                        "AND r.metricDate >= :fromDate " +
                        "AND r.metricDate <= :toDate " +
                        "ORDER BY r.metricDate ASC")
        List<RevenueDailyAggregate> findInstructorTrendRows(@Param("instructorId") String instructorId,
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate);

        @Query("SELECT r FROM RevenueDailyAggregate r " +
                        "WHERE (:instructorId IS NULL OR r.instructorId = :instructorId) " +
                        "AND r.metricDate >= :fromDate " +
                        "AND r.metricDate <= :toDate " +
                        "ORDER BY r.metricDate ASC")
        List<RevenueDailyAggregate> findAdminTrendRows(@Param("instructorId") String instructorId,
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate);
}
