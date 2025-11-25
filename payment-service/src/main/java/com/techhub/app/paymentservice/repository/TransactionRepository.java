package com.techhub.app.paymentservice.repository;

import com.techhub.app.paymentservice.entity.Transaction;
import com.techhub.app.paymentservice.entity.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByUserIdAndIsActive(UUID userId, String isActive);

    List<Transaction> findByStatusAndIsActive(TransactionStatus status, String isActive);

    List<Transaction> findByUserIdAndStatusAndIsActive(UUID userId, TransactionStatus status, String isActive);
}

