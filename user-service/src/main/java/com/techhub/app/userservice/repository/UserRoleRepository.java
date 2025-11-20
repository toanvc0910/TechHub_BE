package com.techhub.app.userservice.repository;

import com.techhub.app.userservice.entity.UserRole;
import com.techhub.app.userservice.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {
    @Query("SELECT ur FROM UserRole ur WHERE ur.user.id = :userId AND ur.isActive = true")
    List<UserRole> findByUserId(@Param("userId") UUID userId);
    boolean existsByUserIdAndRoleId(UUID userId, UUID roleId);
    Optional<UserRole> findByUserIdAndRoleId(UUID userId, UUID roleId);
}
