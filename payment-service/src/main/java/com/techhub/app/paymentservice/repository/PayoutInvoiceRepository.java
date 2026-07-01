package com.techhub.app.paymentservice.repository;

import com.techhub.app.paymentservice.entity.PayoutInvoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PayoutInvoiceRepository extends JpaRepository<PayoutInvoice, String> {

    Optional<PayoutInvoice> findByPayoutRequestIdAndIsActive(String payoutRequestId, String isActive);

    List<PayoutInvoice> findByInstructorIdAndIsActiveOrderByCreatedDesc(String instructorId, String isActive);

    List<PayoutInvoice> findByIsActiveOrderByCreatedDesc(String isActive);
}