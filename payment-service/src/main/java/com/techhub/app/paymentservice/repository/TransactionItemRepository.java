package com.techhub.app.paymentservice.repository;

import com.techhub.app.paymentservice.entity.TransactionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionItemRepository extends JpaRepository<TransactionItem, UUID> {

    List<TransactionItem> findByTransactionIdAndIsActive(UUID transactionId, String isActive);

    List<TransactionItem> findByCourseIdAndIsActive(UUID courseId, String isActive);
}

