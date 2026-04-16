package com.techhub.app.paymentservice.repository;

import com.techhub.app.paymentservice.entity.Payment;
import com.techhub.app.paymentservice.entity.enums.PaymentMethod;
import com.techhub.app.paymentservice.entity.enums.PaymentStatus;
import com.techhub.app.paymentservice.repository.projection.PaymentHistoryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    @Query(value = "SELECT " +
            "p.id AS id, " +
            "t.id AS transactionId, " +
            "t.user_id AS userId, " +
            "u.username AS userName, " +
            "u.email AS userEmail, " +
            "MIN(c.id) AS courseId, " +
            "CASE WHEN COUNT(DISTINCT c.id) = 1 THEN MIN(c.title) ELSE 'Multiple courses' END AS courseName, " +
            "COALESCE(SUM(ti.price_at_purchase * COALESCE(ti.quantity, 1)), 0) AS grossAmount, " +
            "CAST(p.method AS TEXT) AS paymentMethod, " +
            "CAST(p.status AS TEXT) AS status, " +
            "p.created AS created, " +
            "p.updated AS updated " +
            "FROM payments p " +
            "JOIN transactions t ON t.id = p.transaction_id " +
            "LEFT JOIN users u ON u.id = t.user_id " +
            "LEFT JOIN transaction_items ti ON ti.transaction_id = t.id AND ti.is_active = 'Y' " +
            "LEFT JOIN courses c ON c.id = ti.course_id AND c.is_active = 'Y' " +
            "WHERE p.is_active = 'Y' AND t.is_active = 'Y' " +
            "GROUP BY p.id, t.id, t.user_id, u.username, u.email, p.method, p.status, p.created, p.updated " +
            "ORDER BY p.created DESC", countQuery = "SELECT COUNT(*) FROM payments p WHERE p.is_active = 'Y'", nativeQuery = true)
    Page<PaymentHistoryProjection> findPaymentHistoryForAdmin(Pageable pageable);

    @Query(value = "SELECT " +
            "p.id AS id, " +
            "t.id AS transactionId, " +
            "t.user_id AS userId, " +
            "u.username AS userName, " +
            "u.email AS userEmail, " +
            "MIN(c.id) AS courseId, " +
            "CASE WHEN COUNT(DISTINCT c.id) = 1 THEN MIN(c.title) ELSE 'Multiple courses' END AS courseName, " +
            "COALESCE(SUM(ti.price_at_purchase * COALESCE(ti.quantity, 1)), 0) AS grossAmount, " +
            "CAST(p.method AS TEXT) AS paymentMethod, " +
            "CAST(p.status AS TEXT) AS status, " +
            "p.created AS created, " +
            "p.updated AS updated " +
            "FROM payments p " +
            "JOIN transactions t ON t.id = p.transaction_id AND t.is_active = 'Y' " +
            "LEFT JOIN users u ON u.id = t.user_id " +
            "JOIN transaction_items ti ON ti.transaction_id = t.id AND ti.is_active = 'Y' " +
            "JOIN courses c ON c.id = ti.course_id AND c.is_active = 'Y' " +
            "WHERE p.is_active = 'Y' " +
            "AND c.instructor_id = CAST(:instructorId AS UUID) " +
            "GROUP BY p.id, t.id, t.user_id, u.username, u.email, p.method, p.status, p.created, p.updated " +
            "ORDER BY p.created DESC", countQuery = "SELECT COUNT(DISTINCT p.id) " +
                    "FROM payments p " +
                    "JOIN transactions t ON t.id = p.transaction_id AND t.is_active = 'Y' " +
                    "JOIN transaction_items ti ON ti.transaction_id = t.id AND ti.is_active = 'Y' " +
                    "JOIN courses c ON c.id = ti.course_id AND c.is_active = 'Y' " +
                    "WHERE p.is_active = 'Y' AND c.instructor_id = CAST(:instructorId AS UUID)", nativeQuery = true)
    Page<PaymentHistoryProjection> findPaymentHistoryForInstructor(@Param("instructorId") UUID instructorId,
            Pageable pageable);

    Page<Payment> findByIsActive(String isActive, Pageable pageable);

    java.util.Optional<Payment> findByIdAndIsActive(UUID id, String isActive);

    List<Payment> findByTransactionIdAndIsActive(UUID transactionId, String isActive);

    List<Payment> findByMethodAndIsActive(PaymentMethod method, String isActive);

    List<Payment> findByStatusAndIsActive(PaymentStatus status, String isActive);
}
