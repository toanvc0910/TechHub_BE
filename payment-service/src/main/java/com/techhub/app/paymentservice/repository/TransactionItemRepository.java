package com.techhub.app.paymentservice.repository;

import com.techhub.app.paymentservice.entity.TransactionItem;
import com.techhub.app.paymentservice.repository.projection.RevenueByCourseProjection;
import com.techhub.app.paymentservice.repository.projection.RevenueOverviewProjection;
import com.techhub.app.paymentservice.repository.projection.RevenueSplitItemProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionItemRepository extends JpaRepository<TransactionItem, UUID> {

        List<TransactionItem> findByTransactionIdAndIsActive(UUID transactionId, String isActive);

        List<TransactionItem> findByCourseIdAndIsActive(UUID courseId, String isActive);

        @Query(value = "SELECT " +
                        "COALESCE(SUM(ti.price_at_purchase * ti.quantity), 0) AS grossRevenue, " +
                        "COUNT(DISTINCT t.id) AS totalOrders, " +
                        "COALESCE(SUM(ti.quantity), 0) AS totalItems, " +
                        "COUNT(DISTINCT ti.course_id) AS totalCourses " +
                        "FROM transaction_items ti " +
                        "JOIN transactions t ON t.id = ti.transaction_id " +
                        "JOIN courses c ON c.id = ti.course_id " +
                        "WHERE ti.is_active = 'Y' " +
                        "AND t.is_active = 'Y' " +
                        "AND c.is_active = 'Y' " +
                        "AND t.status = 'COMPLETED' " +
                        "AND c.instructor_id = CAST(:instructorId AS UUID) " +
                        "AND (CAST(:fromDate AS TIMESTAMPTZ) IS NULL OR t.created >= CAST(:fromDate AS TIMESTAMPTZ)) " +
                        "AND (CAST(:toDate AS TIMESTAMPTZ) IS NULL OR t.created < CAST(:toDate AS TIMESTAMPTZ))", nativeQuery = true)
        RevenueOverviewProjection getInstructorRevenueOverview(@Param("instructorId") UUID instructorId,
                        @Param("fromDate") OffsetDateTime fromDate,
                        @Param("toDate") OffsetDateTime toDate);

        @Query(value = "SELECT " +
                        "COALESCE(SUM(ti.price_at_purchase * ti.quantity), 0) AS grossRevenue, " +
                        "COUNT(DISTINCT t.id) AS totalOrders, " +
                        "COALESCE(SUM(ti.quantity), 0) AS totalItems, " +
                        "COUNT(DISTINCT ti.course_id) AS totalCourses " +
                        "FROM transaction_items ti " +
                        "JOIN transactions t ON t.id = ti.transaction_id " +
                        "JOIN courses c ON c.id = ti.course_id " +
                        "WHERE ti.is_active = 'Y' " +
                        "AND t.is_active = 'Y' " +
                        "AND c.is_active = 'Y' " +
                        "AND t.status = 'COMPLETED' " +
                        "AND (CAST(:instructorId AS UUID) IS NULL OR c.instructor_id = CAST(:instructorId AS UUID)) " +
                        "AND (CAST(:fromDate AS TIMESTAMPTZ) IS NULL OR t.created >= CAST(:fromDate AS TIMESTAMPTZ)) " +
                        "AND (CAST(:toDate AS TIMESTAMPTZ) IS NULL OR t.created < CAST(:toDate AS TIMESTAMPTZ))", nativeQuery = true)
        RevenueOverviewProjection getAdminRevenueOverview(@Param("instructorId") UUID instructorId,
                        @Param("fromDate") OffsetDateTime fromDate,
                        @Param("toDate") OffsetDateTime toDate);

        @Query(value = "SELECT " +
                        "ti.course_id AS courseId, " +
                        "c.title AS courseTitle, " +
                        "COALESCE(SUM(ti.price_at_purchase * ti.quantity), 0) AS grossRevenue, " +
                        "COALESCE(SUM(ti.quantity), 0) AS soldCount, " +
                        "COUNT(DISTINCT t.id) AS orderCount " +
                        "FROM transaction_items ti " +
                        "JOIN transactions t ON t.id = ti.transaction_id " +
                        "JOIN courses c ON c.id = ti.course_id " +
                        "WHERE ti.is_active = 'Y' " +
                        "AND t.is_active = 'Y' " +
                        "AND c.is_active = 'Y' " +
                        "AND t.status = 'COMPLETED' " +
                        "AND c.instructor_id = CAST(:instructorId AS UUID) " +
                        "AND (CAST(:fromDate AS TIMESTAMPTZ) IS NULL OR t.created >= CAST(:fromDate AS TIMESTAMPTZ)) " +
                        "AND (CAST(:toDate AS TIMESTAMPTZ) IS NULL OR t.created < CAST(:toDate AS TIMESTAMPTZ)) " +
                        "GROUP BY ti.course_id, c.title " +
                        "ORDER BY grossRevenue DESC", nativeQuery = true)
        List<RevenueByCourseProjection> getInstructorRevenueByCourse(@Param("instructorId") UUID instructorId,
                        @Param("fromDate") OffsetDateTime fromDate,
                        @Param("toDate") OffsetDateTime toDate);

        @Query(value = "SELECT " +
                        "CAST(ti.course_id AS TEXT) AS courseId, " +
                        "CAST(c.instructor_id AS TEXT) AS instructorId, " +
                        "(ti.price_at_purchase * ti.quantity) AS grossAmount, " +
                        "COALESCE(ti.quantity, 1) AS quantity " +
                        "FROM transaction_items ti " +
                        "JOIN courses c ON c.id = ti.course_id " +
                        "WHERE ti.transaction_id = :transactionId " +
                        "AND ti.is_active = 'Y' " +
                        "AND c.is_active = 'Y'", nativeQuery = true)
        List<RevenueSplitItemProjection> getRevenueSplitItemsByTransactionId(
                        @Param("transactionId") UUID transactionId);
}
