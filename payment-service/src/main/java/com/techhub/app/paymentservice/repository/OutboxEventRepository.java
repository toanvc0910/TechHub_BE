package com.techhub.app.paymentservice.repository;

import com.techhub.app.paymentservice.entity.OutboxEvent;
import com.techhub.app.paymentservice.entity.enums.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

        Optional<OutboxEvent> findByEventKey(String eventKey);

        List<OutboxEvent> findTop100ByStatusOrderByCreatedAsc(OutboxStatus status);

        List<OutboxEvent> findTop100ByStatusAndRetryCountLessThanOrderByCreatedAsc(OutboxStatus status,
                        int maxRetryCount);

        @Modifying
        @Query(value = "UPDATE outbox_events SET status = :status, published_at = :publishedAt, " +
                        "last_error = NULL, retry_count = :retryCount, updated = NOW() " +
                        "WHERE id = :id", nativeQuery = true)
        int markPublished(@Param("id") String id, @Param("status") String status,
                        @Param("retryCount") int retryCount,
                        @Param("publishedAt") OffsetDateTime publishedAt);

        @Modifying
        @Query(value = "UPDATE outbox_events SET status = :status, retry_count = :retryCount, " +
                        "last_error = :lastError, updated = NOW() " +
                        "WHERE id = :id", nativeQuery = true)
        int markFailed(@Param("id") String id, @Param("status") String status,
                        @Param("retryCount") int retryCount,
                        @Param("lastError") String lastError);
}