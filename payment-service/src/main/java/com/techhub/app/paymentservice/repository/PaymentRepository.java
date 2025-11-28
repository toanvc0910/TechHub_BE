package com.techhub.app.paymentservice.repository;

import com.techhub.app.paymentservice.entity.Payment;
import com.techhub.app.paymentservice.entity.enums.PaymentMethod;
import com.techhub.app.paymentservice.entity.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByTransactionIdAndIsActive(UUID transactionId, String isActive);

    List<Payment> findByMethodAndIsActive(PaymentMethod method, String isActive);

    List<Payment> findByStatusAndIsActive(PaymentStatus status, String isActive);
}

