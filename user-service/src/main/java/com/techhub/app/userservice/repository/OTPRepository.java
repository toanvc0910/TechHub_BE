package com.techhub.app.userservice.repository;

import com.techhub.app.userservice.entity.OTP;
import com.techhub.app.userservice.enums.OtpType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OTPRepository extends JpaRepository<OTP, UUID> {

    Optional<OTP> findByCodeAndTypeAndIsActiveTrueAndExpiresAtAfter(
        String code, OtpType type, LocalDateTime currentTime);

    List<OTP> findByUser_IdAndTypeAndIsActiveTrueAndExpiresAtAfter(
        UUID userId, OtpType type, LocalDateTime currentTime);

    @Modifying
    @Query("UPDATE OTP o SET o.isActive = false WHERE o.user.id = :userId AND o.type = :type AND o.isActive = true")
    void deactivateAllByUserIdAndType(@Param("userId") UUID userId, @Param("type") OtpType type);

    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
