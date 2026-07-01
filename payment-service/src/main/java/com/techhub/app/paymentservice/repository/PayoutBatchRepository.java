package com.techhub.app.paymentservice.repository;

import com.techhub.app.paymentservice.entity.PayoutBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayoutBatchRepository extends JpaRepository<PayoutBatch, String> {

    Optional<PayoutBatch> findByPeriodKeyAndBatchNameAndIsActive(String periodKey, String batchName, String isActive);

    List<PayoutBatch> findByIsActiveOrderByCreatedDesc(String isActive);
}
