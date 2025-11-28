package com.techhub.app.notificationservice.repository;

import com.techhub.app.notificationservice.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByUserIdAndIsActiveTrue(UUID userId, Pageable pageable);

    Page<Notification> findByUserIdAndIsActiveTrueAndRead(UUID userId, boolean read, Pageable pageable);

    Optional<Notification> findByIdAndUserIdAndIsActiveTrue(UUID id, UUID userId);

    long countByUserIdAndIsActiveTrueAndRead(UUID userId, boolean read);

    @Modifying
    @Query("update Notification n set n.read = true, n.updated = :updatedAt where n.userId = :userId and n.read = false")
    int markAllAsRead(UUID userId, OffsetDateTime updatedAt);
}
