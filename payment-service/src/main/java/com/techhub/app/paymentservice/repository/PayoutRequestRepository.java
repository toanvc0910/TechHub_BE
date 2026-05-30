package com.techhub.app.paymentservice.repository;

import com.techhub.app.paymentservice.entity.PayoutRequest;
import com.techhub.app.paymentservice.entity.enums.PayoutRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayoutRequestRepository extends JpaRepository<PayoutRequest, String> {

    @Query(value = "SELECT * FROM payout_requests pr WHERE CAST(pr.id AS TEXT) = CAST(:id AS TEXT) " +
            "AND pr.is_active = 'Y' LIMIT 1", nativeQuery = true)
        Optional<PayoutRequest> findActiveById(@Param("id") String id);

    @Query(value = "SELECT * FROM payout_requests pr WHERE CAST(pr.instructor_id AS TEXT) = :instructorId " +
            "AND pr.is_active = :isActive ORDER BY pr.created DESC", nativeQuery = true)
    List<PayoutRequest> findByInstructorIdAndIsActiveOrderByCreatedDesc(@Param("instructorId") String instructorId,
            @Param("isActive") String isActive);

    List<PayoutRequest> findByIsActiveOrderByCreatedDesc(String isActive);

    @Query(value = "SELECT * FROM payout_requests pr WHERE pr.batch_id IS NULL " +
            "AND pr.status = 'REQUESTED' AND pr.is_active = 'Y' " +
            "AND pr.created >= :fromDate AND pr.created < :toDate ORDER BY pr.created " +
            "FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<PayoutRequest> findUnbatchedRequestedInRange(@Param("fromDate") OffsetDateTime fromDate,
            @Param("toDate") OffsetDateTime toDate);

    @Query(value = "SELECT COALESCE(SUM(pr.amount), 0) FROM payout_requests pr " +
            "WHERE CAST(pr.instructor_id AS TEXT) = :instructorId " +
            "AND pr.status IN (:statuses) AND pr.is_active = 'Y'", nativeQuery = true)
    BigDecimal sumAmountByInstructorAndStatuses(@Param("instructorId") String instructorId,
            @Param("statuses") Collection<String> statuses);
}
