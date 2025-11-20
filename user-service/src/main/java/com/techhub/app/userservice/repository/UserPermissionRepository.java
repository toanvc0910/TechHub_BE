package com.techhub.app.userservice.repository;

import com.techhub.app.userservice.entity.UserPermission;
import com.techhub.app.userservice.entity.UserPermissionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserPermissionRepository extends JpaRepository<UserPermission, UserPermissionId> {

    @Query("SELECT up FROM UserPermission up WHERE up.userId = :userId AND up.isActive = true")
    List<UserPermission> findActiveByUserId(@Param("userId") UUID userId);

    Optional<UserPermission> findByUserIdAndPermissionId(UUID userId, UUID permissionId);
}
