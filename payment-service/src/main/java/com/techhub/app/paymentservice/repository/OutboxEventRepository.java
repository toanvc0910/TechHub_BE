package com.techhub.app.paymentservice.repository;

import com.techhub.app.paymentservice.entity.OutboxEvent;
import com.techhub.app.paymentservice.entity.enums.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    Optional<OutboxEvent> findByEventKey(String eventKey);

    List<OutboxEvent> findTop100ByStatusOrderByCreatedAsc(OutboxStatus status);
}