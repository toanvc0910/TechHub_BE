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
                        "AND r.metricDate >= :fromDate " +
                        "AND r.metricDate <= :toDate")
        BigDecimal sumInstructorGrossRevenue(@Param("instructorId") UUID instructorId,
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate);

        @Query("SELECT COALESCE(SUM(r.instructorRevenue), 0) FROM RevenueDailyAggregate r " +
                        "WHERE r.instructorId = :instructorId " +
                        "AND r.metricDate >= :fromDate " +
                        "AND r.metricDate <= :toDate")
        BigDecimal sumInstructorNetRevenue(@Param("instructorId") UUID instructorId,
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate);

        // Native + CAST(:instructorId AS uuid) so Postgres can type a null bind (fixes 42P18).
        @Query(value = "SELECT COALESCE(SUM(r.instructor_revenue), 0) FROM analytics_revenue_daily r " +
                        "WHERE (CAST(:instructorId AS uuid) IS NULL OR r.instructor_id = CAST(:instructorId AS uuid)) " +
                        "AND r.metric_date >= :fromDate " +
                        "AND r.metric_date <= :toDate", nativeQuery = true)
        BigDecimal sumAdminInstructorNetRevenue(@Param("instructorId") UUID instructorId,
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate);

        @Query(value = "SELECT COALESCE(SUM(r.gross_revenue), 0) FROM analytics_revenue_daily r " +
                        "WHERE (CAST(:instructorId AS uuid) IS NULL OR r.instructor_id = CAST(:instructorId AS uuid)) " +
                        "AND r.metric_date >= :fromDate " +
                        "AND r.metric_date <= :toDate", nativeQuery = true)
        BigDecimal sumAdminGrossRevenue(@Param("instructorId") UUID instructorId,
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate);

        @Query(value = "SELECT COALESCE(SUM(r.admin_revenue), 0) FROM analytics_revenue_daily r " +
                        "WHERE (CAST(:instructorId AS uuid) IS NULL OR r.instructor_id = CAST(:instructorId AS uuid)) " +
                        "AND r.metric_date >= :fromDate " +
                        "AND r.metric_date <= :toDate", nativeQuery = true)
        BigDecimal sumAdminNetRevenue(@Param("instructorId") UUID instructorId,
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate);

        @Query(value = "SELECT CAST(COALESCE(SUM(r.order_count), 0) AS bigint) FROM analytics_revenue_daily r " +
                        "WHERE (CAST(:instructorId AS uuid) IS NULL OR r.instructor_id = CAST(:instructorId AS uuid)) " +
                        "AND r.metric_date >= :fromDate " +
                        "AND r.metric_date <= :toDate", nativeQuery = true)
        Long sumOrderCount(@Param("instructorId") UUID instructorId,
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate);

        @Query(value = "SELECT CAST(COALESCE(SUM(r.item_count), 0) AS bigint) FROM analytics_revenue_daily r " +
                        "WHERE (CAST(:instructorId AS uuid) IS NULL OR r.instructor_id = CAST(:instructorId AS uuid)) " +
                        "AND r.metric_date >= :fromDate " +
                        "AND r.metric_date <= :toDate", nativeQuery = true)
        Long sumItemCount(@Param("instructorId") UUID instructorId,
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate);

        @Query("SELECT r FROM RevenueDailyAggregate r " +
                        "WHERE r.instructorId = :instructorId " +
                        "AND r.metricDate >= :fromDate " +
                        "AND r.metricDate <= :toDate " +
                        "ORDER BY r.metricDate ASC")
        List<RevenueDailyAggregate> findInstructorTrendRows(@Param("instructorId") UUID instructorId,
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate);

        // Admin trend always covers all instructors (instructor-specific requests are routed
        // to findInstructorTrendRows), so no instructor filter is needed here. Keeping the
        // nullable :instructorId predicate caused Postgres error 42P18 (untyped null param).
        @Query("SELECT r FROM RevenueDailyAggregate r " +
                        "WHERE r.metricDate >= :fromDate " +
                        "AND r.metricDate <= :toDate " +
                        "ORDER BY r.metricDate ASC")
        List<RevenueDailyAggregate> findAdminTrendRows(@Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate);
}
