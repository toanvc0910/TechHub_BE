package com.techhub.app.aiservice.repository;

import com.techhub.app.aiservice.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {

    Optional<ChatSession> findByIdAndUserId(UUID id, UUID userId);

    List<ChatSession> findByUserIdOrderByStartedAtDesc(UUID userId);

    @Modifying
    @Query("DELETE FROM ChatSession s WHERE s.startedAt < :cutoffDate")
    void deleteOldSessions(LocalDateTime cutoffDate);
}
