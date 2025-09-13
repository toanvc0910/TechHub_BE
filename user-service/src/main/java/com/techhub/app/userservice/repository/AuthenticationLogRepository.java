package com.techhub.app.userservice.repository;

import com.techhub.app.userservice.entity.AuthenticationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuthenticationLogRepository extends JpaRepository<AuthenticationLog, UUID> {

    Page<AuthenticationLog> findByUserIdOrderByLoginTimeDesc(UUID userId, Pageable pageable);

    List<AuthenticationLog> findByUserIdAndLoginTimeBetween(
        UUID userId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(a) FROM AuthenticationLog a WHERE a.user.id = :userId " +
           "AND a.success = false AND a.loginTime > :since")
    long countFailedLoginAttempts(@Param("userId") UUID userId, @Param("since") LocalDateTime since);

    void deleteByLoginTimeBefore(LocalDateTime dateTime);
}
