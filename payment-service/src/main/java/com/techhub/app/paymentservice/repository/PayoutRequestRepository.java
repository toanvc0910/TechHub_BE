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

    List<PayoutRequest> findByInstructorIdAndIsActiveOrderByCreatedDesc(UUID instructorId, String isActive);

    List<PayoutRequest> findByIsActiveOrderByCreatedDesc(String isActive);

    @Query("SELECT COALESCE(SUM(pr.amount), 0) FROM PayoutRequest pr WHERE pr.instructorId = :instructorId AND pr.status IN :statuses AND pr.isActive = 'Y'")
    BigDecimal sumAmountByInstructorAndStatuses(@Param("instructorId") UUID instructorId,
            @Param("statuses") Collection<PayoutRequestStatus> statuses);
}
