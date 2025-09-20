package com.techhub.app.userservice.repository;

import com.techhub.app.userservice.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, UUID> {

    @Query("SELECT p FROM Profile p WHERE p.user.id = :userId")
    Optional<Profile> findByUserId(@Param("userId") UUID userId);

    @Query("SELECT p FROM Profile p WHERE p.user.id = :userId AND p.isActive = true")
    Optional<Profile> findByUserIdAndIsActiveTrue(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM Profile p WHERE p.user.id = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}
