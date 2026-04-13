package com.techhub.app.paymentservice.repository;

import com.techhub.app.paymentservice.entity.PayoutRequest;
import com.techhub.app.paymentservice.entity.enums.PayoutRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface PayoutRequestRepository extends JpaRepository<PayoutRequest, UUID> {

    @Query(value = "SELECT * FROM payout_requests pr WHERE CAST(pr.instructor_id AS TEXT) = :instructorId " +
            "AND pr.is_active = :isActive ORDER BY pr.created DESC", nativeQuery = true)
    List<PayoutRequest> findByInstructorIdAndIsActiveOrderByCreatedDesc(@Param("instructorId") String instructorId,
            @Param("isActive") String isActive);

    List<PayoutRequest> findByIsActiveOrderByCreatedDesc(String isActive);

    @Query(value = "SELECT COALESCE(SUM(pr.amount), 0) FROM payout_requests pr " +
            "WHERE CAST(pr.instructor_id AS TEXT) = :instructorId " +
            "AND pr.status IN (:statuses) AND pr.is_active = 'Y'", nativeQuery = true)
    BigDecimal sumAmountByInstructorAndStatuses(@Param("instructorId") String instructorId,
            @Param("statuses") Collection<String> statuses);
}
