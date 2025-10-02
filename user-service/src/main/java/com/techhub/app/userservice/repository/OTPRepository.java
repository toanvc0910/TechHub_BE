package com.techhub.app.userservice.repository;

import com.techhub.app.userservice.entity.OTPCode;
import com.techhub.app.userservice.enums.OTPTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OTPRepository extends JpaRepository<OTPCode, UUID> {

    Optional<OTPCode> findByUserIdAndCodeAndTypeAndIsActiveTrue(UUID userId, String code, OTPTypeEnum type);

    Optional<OTPCode> findByUserIdAndTypeAndIsActiveTrue(UUID userId, OTPTypeEnum type);

    @Modifying
    @Query("UPDATE OTPCode o SET o.isActive = false WHERE o.userId = :userId AND o.type = :type")
    void deactivateByUserIdAndType(@Param("userId") UUID userId, @Param("type") OTPTypeEnum type);

    @Modifying
    @Query("DELETE FROM OTPCode o WHERE o.expiresAt < :now")
    void deleteExpiredOTPs(@Param("now") LocalDateTime now);

    boolean existsByUserIdAndTypeAndIsActiveTrueAndExpiresAtAfter(UUID userId, OTPTypeEnum type, LocalDateTime now);
}
