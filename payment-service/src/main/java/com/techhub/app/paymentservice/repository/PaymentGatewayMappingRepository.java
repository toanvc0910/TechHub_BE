package com.techhub.app.paymentservice.repository;

import com.techhub.app.paymentservice.entity.PaymentGatewayMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentGatewayMappingRepository extends JpaRepository<PaymentGatewayMapping, UUID> {
    Optional<PaymentGatewayMapping> findByGatewayOrderId(String gatewayOrderId);
}

