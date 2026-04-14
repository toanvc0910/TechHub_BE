package com.techhub.app.paymentservice.repository;

import com.techhub.app.paymentservice.entity.Payment;
import com.techhub.app.paymentservice.entity.enums.PaymentMethod;
import com.techhub.app.paymentservice.entity.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Page<Payment> findByIsActive(String isActive, Pageable pageable);

    java.util.Optional<Payment> findByIdAndIsActive(UUID id, String isActive);

    List<Payment> findByTransactionIdAndIsActive(UUID transactionId, String isActive);

    List<Payment> findByMethodAndIsActive(PaymentMethod method, String isActive);

    List<Payment> findByStatusAndIsActive(PaymentStatus status, String isActive);
}
